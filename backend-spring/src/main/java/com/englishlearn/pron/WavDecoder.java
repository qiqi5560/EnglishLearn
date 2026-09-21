package com.englishlearn.pron;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 极简 WAV 解析：把浏览器上传的 16bit PCM 单声道 WAV 解成 float 数组，
 * 并在采样率不是 16kHz 时线性重采样（音素模型要求 16kHz）。
 *
 * <p>只支持无压缩 PCM（format 1）与 32 位浮点（format 3），
 * 浏览器端已固定按 16bit PCM 编码，这里保持最小实现、不引入音频依赖。
 */
public final class WavDecoder {

    /** 模型要求的采样率 */
    public static final int TARGET_RATE = 16000;

    private WavDecoder() {
    }

    /** 解码结果：16kHz 单声道波形 + 时长（秒） */
    public record Pcm(float[] samples, double duration) {
    }

    public static Pcm decode(byte[] wav) throws IOException {
        if (wav == null || wav.length < 44) {
            throw new IOException("音频过短，可能不是有效的 WAV");
        }
        String riff = new String(wav, 0, 4, StandardCharsets.US_ASCII);
        if (!"RIFF".equals(riff)) {
            throw new IOException("只支持 WAV 格式音频（收到 " + riff + "）");
        }
        ByteArrayInputStream in = new ByteArrayInputStream(wav);
        in.skip(12); // RIFF 头

        int audioFormat = 1;
        int channels = 1;
        int sampleRate = TARGET_RATE;
        int bitsPerSample = 16;
        byte[] data = null;

        while (in.available() >= 8) {
            byte[] head = in.readNBytes(4);
            int size = readLeInt(in.readNBytes(4));
            String id = new String(head, StandardCharsets.US_ASCII);
            if ("fmt ".equals(id)) {
                byte[] fmt = in.readNBytes(size);
                if (fmt.length >= 16) {
                    audioFormat = readLeShort(fmt, 0);
                    channels = readLeShort(fmt, 2);
                    sampleRate = readLeInt(java.util.Arrays.copyOfRange(fmt, 4, 8));
                    bitsPerSample = readLeShort(fmt, 14);
                }
            } else if ("data".equals(id)) {
                data = in.readNBytes(size);
                break;
            } else {
                in.skip(size);
                if ((size & 1) == 1) {
                    in.skip(1); // chunk 按偶数字节对齐
                }
            }
        }
        if (data == null || data.length == 0) {
            throw new IOException("WAV 中没有音频数据");
        }

        float[] raw = toFloat(data, audioFormat, bitsPerSample);
        float[] mono = downmix(raw, Math.max(1, channels));
        float[] resampled = sampleRate == TARGET_RATE ? mono : resample(mono, sampleRate, TARGET_RATE);
        return new Pcm(resampled, resampled.length / (double) TARGET_RATE);
    }

    private static float[] toFloat(byte[] data, int audioFormat, int bitsPerSample) {
        int bytesPerSample = Math.max(1, bitsPerSample / 8);
        int count = data.length / bytesPerSample;
        float[] out = new float[count];
        if (audioFormat == 3 && bitsPerSample == 32) {
            for (int i = 0; i < count; i++) {
                int bits = readLeInt(java.util.Arrays.copyOfRange(data, i * 4, i * 4 + 4));
                out[i] = Float.intBitsToFloat(bits);
            }
            return out;
        }
        for (int i = 0; i < count; i++) {
            int off = i * bytesPerSample;
            if (bitsPerSample == 8) {
                out[i] = (data[off] - 128) / 128f; // 8 位 WAV 是无符号
            } else {
                int v = data[off] & 0xFF;
                if (off + 1 < data.length) {
                    v |= (data[off + 1] & 0xFF) << 8;
                }
                out[i] = (short) v / 32768f;
            }
        }
        return out;
    }

    /** 多声道取平均，统一成单声道 */
    private static float[] downmix(float[] src, int channels) {
        if (channels == 1) {
            return src;
        }
        int frames = src.length / channels;
        float[] out = new float[frames];
        for (int i = 0; i < frames; i++) {
            float sum = 0;
            for (int c = 0; c < channels; c++) {
                sum += src[i * channels + c];
            }
            out[i] = sum / channels;
        }
        return out;
    }

    /** 线性插值重采样 */
    private static float[] resample(float[] src, int srcRate, int dstRate) {
        if (srcRate == dstRate || src.length == 0) {
            return src;
        }
        int outLen = (int) Math.round(src.length * (double) dstRate / srcRate);
        float[] out = new float[outLen];
        double step = srcRate / (double) dstRate;
        for (int i = 0; i < outLen; i++) {
            double pos = i * step;
            int i0 = (int) Math.floor(pos);
            int i1 = Math.min(i0 + 1, src.length - 1);
            double frac = pos - i0;
            float v0 = src[Math.min(i0, src.length - 1)];
            float v1 = src[i1];
            out[i] = (float) (v0 + (v1 - v0) * frac);
        }
        return out;
    }

    /** 零均值单位方差归一化，与模型训练时的输入分布一致 */
    public static float[] normalize(float[] samples) {
        if (samples.length == 0) {
            return samples;
        }
        double sum = 0;
        for (float s : samples) {
            sum += s;
        }
        double mean = sum / samples.length;
        double var = 0;
        for (float s : samples) {
            double d = s - mean;
            var += d * d;
        }
        var /= samples.length;
        double std = Math.sqrt(var + 1e-7);
        float[] out = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            out[i] = (float) ((samples[i] - mean) / std);
        }
        return out;
    }

    private static int readLeShort(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static int readLeInt(byte[] b) {
        if (b.length < 4) {
            return 0;
        }
        return (b[0] & 0xFF) | ((b[1] & 0xFF) << 8) | ((b[2] & 0xFF) << 16) | ((b[3] & 0xFF) << 24);
    }
}
