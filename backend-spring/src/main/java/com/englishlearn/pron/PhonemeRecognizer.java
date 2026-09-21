package com.englishlearn.pron;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.ValueInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 音素识别器：用本地 ONNX 模型（wav2vec2-xls-r-300m 在 TIMIT 上微调）把 16kHz 语音
 * 识别成音素序列。CPU 推理，无需联网、无需 PyTorch。
 */
public final class PhonemeRecognizer implements AutoCloseable {

    /** wav2vec2 卷积层下采样 320 倍，每帧约 20ms */
    private static final double FRAME_STRIDE = 0.02;
    /** 短于该采样点数的音频视为无效语音 */
    private static final int MIN_SAMPLES = 400;
    private static final Set<String> IGNORED = Set.of("[PAD]", "[UNK]", "|", " ");

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String[] id2phoneme;
    private final int blankId;
    private final List<String> inputNames;
    private final Map<String, OnnxJavaType> inputTypes = new LinkedHashMap<>();

    /** 识别出的一个音素（含时间位置与置信度） */
    public record PhonemeHit(String phoneme, double start, double end, double confidence) {
    }

    public PhonemeRecognizer(Path modelPath, Path vocabPath) throws Exception {
        Map<String, Integer> vocab = new ObjectMapper().readValue(
                vocabPath.toFile(), new TypeReference<Map<String, Integer>>() {
                });
        int maxId = 0;
        for (int id : vocab.values()) {
            maxId = Math.max(maxId, id);
        }
        id2phoneme = new String[maxId + 1];
        vocab.forEach((ph, id) -> id2phoneme[id] = ph);
        blankId = vocab.getOrDefault("[PAD]", 0);

        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.setInterOpNumThreads(1);
        opts.setIntraOpNumThreads(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors())));
        session = env.createSession(modelPath.toString(), opts);

        inputNames = new ArrayList<>(session.getInputNames());
        for (String name : inputNames) {
            NodeInfo node = session.getInputInfo().get(name);
            ValueInfo value = node.getInfo();
            if (value instanceof TensorInfo tensor) {
                inputTypes.put(name, tensor.type);
            } else {
                inputTypes.put(name, OnnxJavaType.FLOAT);
            }
        }
    }

    /** 识别音素序列。waveform 需为 16kHz 单声道、已归一化的波形 */
    public List<PhonemeHit> recognize(float[] waveform) {
        List<PhonemeHit> hits = new ArrayList<>();
        if (waveform == null || waveform.length < MIN_SAMPLES) {
            return hits;
        }
        Map<String, OnnxTensor> inputs = new HashMap<>();
        try {
            for (String name : inputNames) {
                boolean isMask = name.toLowerCase().contains("mask");
                OnnxJavaType type = inputTypes.getOrDefault(name, OnnxJavaType.FLOAT);
                if (isMask) {
                    inputs.put(name, maskTensor(type, waveform.length));
                } else {
                    FloatBuffer buffer = FloatBuffer.wrap(waveform);
                    inputs.put(name, OnnxTensor.createTensor(env, buffer, new long[]{1, waveform.length}));
                }
            }
            try (OrtSession.Result result = session.run(inputs)) {
                Object value = result.get(0).getValue();
                float[][] logits = value instanceof float[][][] cube ? cube[0] : new float[0][0];
                if (logits.length == 0) {
                    return hits;
                }
                int prev = -1;
                for (int t = 0; t < logits.length; t++) {
                    float[] frame = logits[t];
                    int best = argmaxWithSoftmax(frame);
                    float conf = softmaxConfidence(frame, best);
                    if (best != prev && best != blankId) {
                        String ph = best < id2phoneme.length ? id2phoneme[best] : null;
                        if (ph != null && !IGNORED.contains(ph)) {
                            hits.add(new PhonemeHit(ph,
                                    round(t * FRAME_STRIDE), round((t + 1) * FRAME_STRIDE), conf));
                        }
                    }
                    prev = best;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("音素模型推理失败: " + e.getMessage(), e);
        } finally {
            for (OnnxTensor tensor : inputs.values()) {
                try {
                    tensor.close();
                } catch (Exception ignored) {
                    // 关闭失败不影响主流程
                }
            }
        }
        return hits;
    }

    private OnnxTensor maskTensor(OnnxJavaType type, int length) throws Exception {
        if (type == OnnxJavaType.BOOL) {
            ByteBuffer buffer = ByteBuffer.allocate(length);
            for (int i = 0; i < length; i++) {
                buffer.put((byte) 1);
            }
            buffer.flip();
            return OnnxTensor.createTensor(env, buffer, new long[]{1, length}, OnnxJavaType.BOOL);
        }
        LongBuffer buffer = LongBuffer.allocate(length);
        for (int i = 0; i < length; i++) {
            buffer.put(1L);
        }
        buffer.flip();
        return OnnxTensor.createTensor(env, buffer, new long[]{1, length});
    }

    private static int argmaxWithSoftmax(float[] frame) {
        int best = 0;
        for (int i = 1; i < frame.length; i++) {
            if (frame[i] > frame[best]) {
                best = i;
            }
        }
        return best;
    }

    private static float softmaxConfidence(float[] frame, int index) {
        float max = frame[0];
        for (float v : frame) {
            max = Math.max(max, v);
        }
        double sum = 0;
        float top = frame[index] - max;
        for (float v : frame) {
            sum += Math.exp(v - max);
        }
        double conf = Math.exp(top) / sum;
        return (float) Math.round(conf * 1000) / 1000f;
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (Exception ignored) {
            // 关闭失败不影响退出
        }
    }
}
