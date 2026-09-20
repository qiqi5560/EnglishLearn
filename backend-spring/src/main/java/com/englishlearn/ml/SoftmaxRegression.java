package com.englishlearn.ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 纯 Java 实现的多项逻辑回归（Softmax Regression）。
 *
 * <p>采用全批量梯度下降 + L2 正则，按固定随机种子做 7:3 训练/验证划分，
 * 验证集准确率用于管理端展示。不依赖任何第三方数值计算库，
 * 适用于本项目样本量较小（数十至数千条）的场景。
 */
public final class SoftmaxRegression {

    private static final double LEARNING_RATE = 0.1;
    private static final int EPOCHS = 500;
    private static final double L2 = 0.01;
    private static final long SEED = 42L;
    private static final double TRAIN_RATIO = 0.7;

    private final int classes;
    private final int dim;
    private final double[][] weights;

    private double accuracy;
    private int trainCount;
    private int testCount;

    public SoftmaxRegression(int classes, int dim) {
        this.classes = classes;
        this.dim = dim;
        this.weights = new double[classes][dim];
    }

    /**
     * 训练模型。训练结束后 {@link #getAccuracy()} 为验证集准确率。
     *
     * @param xs 特征向量（含偏置位）
     * @param ys 类别下标
     */
    public void fit(List<double[]> xs, List<Integer> ys) {
        if (xs == null || ys == null || xs.isEmpty() || xs.size() != ys.size()) {
            throw new IllegalArgumentException("训练样本为空或特征与标签数量不一致");
        }
        int n = xs.size();

        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        Collections.shuffle(order, new Random(SEED));

        int cut = (int) Math.round(n * TRAIN_RATIO);
        if (cut < 1) {
            cut = 1;
        }
        // 样本太少时全部用于训练，准确率在训练集上统计
        if (cut > n - 1) {
            cut = n;
        }
        List<Integer> train = order.subList(0, cut);
        List<Integer> test = order.subList(cut, n);
        trainCount = train.size();
        testCount = test.size();

        double[][] grad = new double[classes][dim];
        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            for (int k = 0; k < classes; k++) {
                Arrays.fill(grad[k], 0.0);
            }
            for (int i : train) {
                double[] x = xs.get(i);
                double[] probs = predictProba(x);
                int y = ys.get(i);
                for (int k = 0; k < classes; k++) {
                    double err = (k == y ? 1.0 : 0.0) - probs[k];
                    for (int d = 0; d < dim; d++) {
                        grad[k][d] -= err * x[d];
                    }
                }
            }
            for (int k = 0; k < classes; k++) {
                for (int d = 0; d < dim; d++) {
                    weights[k][d] -= LEARNING_RATE * (grad[k][d] / trainCount + L2 * weights[k][d]);
                }
            }
        }

        List<Integer> eval = test.isEmpty() ? train : test;
        int correct = 0;
        for (int i : eval) {
            if (predict(xs.get(i)) == ys.get(i)) {
                correct++;
            }
        }
        accuracy = eval.isEmpty() ? 0.0 : (double) correct / eval.size();
    }

    /** 返回每个类别的概率，合计为 1 */
    public double[] predictProba(double[] x) {
        double[] z = new double[classes];
        double max = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < classes; k++) {
            double sum = 0.0;
            for (int d = 0; d < dim; d++) {
                sum += weights[k][d] * x[d];
            }
            z[k] = sum;
            if (sum > max) {
                max = sum;
            }
        }
        double[] probs = new double[classes];
        double total = 0.0;
        for (int k = 0; k < classes; k++) {
            probs[k] = Math.exp(z[k] - max);
            total += probs[k];
        }
        for (int k = 0; k < classes; k++) {
            probs[k] /= total;
        }
        return probs;
    }

    /** 返回概率最大的类别下标 */
    public int predict(double[] x) {
        double[] probs = predictProba(x);
        int best = 0;
        for (int k = 1; k < classes; k++) {
            if (probs[k] > probs[best]) {
                best = k;
            }
        }
        return best;
    }

    public int getClasses() {
        return classes;
    }

    public int getDim() {
        return dim;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public int getTrainCount() {
        return trainCount;
    }

    public int getTestCount() {
        return testCount;
    }

    /** 序列化为可存入 TEXT 列的结构（配合 JsonUtil 使用） */
    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("classes", classes);
        out.put("dim", dim);
        List<List<Double>> w = new ArrayList<>(classes);
        for (double[] row : weights) {
            List<Double> r = new ArrayList<>(dim);
            for (double v : row) {
                r.add(v);
            }
            w.add(r);
        }
        out.put("weights", w);
        out.put("accuracy", accuracy);
        out.put("trainCount", trainCount);
        out.put("testCount", testCount);
        return out;
    }

    /** 从反序列化后的结构恢复模型 */
    public static SoftmaxRegression fromMap(Map<String, Object> map) {
        if (map == null) {
            throw new IllegalStateException("模型数据为空");
        }
        int classes = ((Number) map.get("classes")).intValue();
        int dim = ((Number) map.get("dim")).intValue();
        SoftmaxRegression model = new SoftmaxRegression(classes, dim);

        Object raw = map.get("weights");
        if (!(raw instanceof List<?> rows) || rows.size() != classes) {
            throw new IllegalStateException("模型权重维度不匹配");
        }
        for (int k = 0; k < classes; k++) {
            Object rowObj = rows.get(k);
            if (!(rowObj instanceof List<?> row) || row.size() != dim) {
                throw new IllegalStateException("模型权重维度不匹配");
            }
            for (int d = 0; d < dim; d++) {
                model.weights[k][d] = ((Number) row.get(d)).doubleValue();
            }
        }
        if (map.get("accuracy") instanceof Number acc) {
            model.accuracy = acc.doubleValue();
        }
        if (map.get("trainCount") instanceof Number tc) {
            model.trainCount = tc.intValue();
        }
        if (map.get("testCount") instanceof Number ec) {
            model.testCount = ec.intValue();
        }
        return model;
    }
}
