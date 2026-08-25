package com.polygamma.adsdemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.polygamma.adsdemo.conf.BenchmarkConfig;
import com.polygamma.adsdemo.utils.BenchmarkResult;
import com.polygamma.adsdemo.utils.TorusFheBenchmarkRunner;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BenchmarkTestActivity extends Activity {

    private static final String TAG = "PrivacySettingActivity";

    private TextInputEditText etN;
    private TextInputEditText etMsgMod;
    private TextInputEditText etCarryMod;
    private TextInputEditText etLogNoiseB;
    private TextInputEditText etWarmup;
    private TextInputEditText etIterations;
    private RadioGroup rgBenchmarkMode;
    private Button btnRunBenchmark;
    private Button btnReset;
    private TextView tvResult;
    private ExecutorService benchmarkExecutor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark_test);

        benchmarkExecutor = Executors.newSingleThreadExecutor();

        initView();
        initToolbar();
        initListener();
    }


    private void initView() {
        etN = findViewById(R.id.et_n);
        etMsgMod = findViewById(R.id.et_msg_mod);
        etCarryMod = findViewById(R.id.et_carry_mod);
        etLogNoiseB = findViewById(R.id.et_log_noise_b);
        etWarmup = findViewById(R.id.et_warmup);
        etIterations = findViewById(R.id.et_iterations);
        rgBenchmarkMode = findViewById(R.id.rg_benchmark_mode);
        btnRunBenchmark = findViewById(R.id.btn_run_benchmark);
        btnReset = findViewById(R.id.btn_reset);
        tvResult = findViewById(R.id.tv_result);
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initListener() {
        btnRunBenchmark.setOnClickListener(v -> runBenchmark());
        btnReset.setOnClickListener(v -> reset());
    }

    private void runBenchmark() {

        BenchmarkConfig config;

        try {
            config = createBenchmarkConfig();
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Invalid benchmark configuration: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        setBenchmarkRunning(true);

        tvResult.setText(
                "Benchmark running...\n\n" +
                        formatConfig(config)
        );

        benchmarkExecutor.execute(() -> {
            try {
                TorusFheBenchmarkRunner runner =
                        new TorusFheBenchmarkRunner();
                BenchmarkResult result = runner.run(config);
                runOnUiThread(() -> {
                    tvResult.setText(
                            formatResult(config, result)
                    );
                    setBenchmarkRunning(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvResult.setText(
                            "BENCHMARK FAILED\n\n" +
                                    e.getClass().getSimpleName() +
                                    "\n" +
                                    e.getMessage()
                    );
                    setBenchmarkRunning(false);
                });
            }
        });
    }

    private BenchmarkConfig createBenchmarkConfig() {

        int n = getIntValue(etN, "n");
        int msgMod = getIntValue(etMsgMod, "msgMod");
        int carryMod = getIntValue(etCarryMod, "carryMod");
        int logNoiseB = getIntValue(etLogNoiseB, "logNoiseB");
        int warmup = getIntValue(etWarmup, "warmup");
        int iterations = getIntValue(etIterations, "iterations");
        BenchmarkConfig.Mode mode;

        int checkedId = rgBenchmarkMode.getCheckedRadioButtonId();

        if (checkedId == R.id.rb_encrypt) {
            mode = BenchmarkConfig.Mode.ENCRYPT;
        } else if (checkedId == R.id.rb_decrypt) {
            mode = BenchmarkConfig.Mode.DECRYPT;
        } else {
            mode = BenchmarkConfig.Mode.FULL;
        }

        validateConfig(
                n,
                msgMod,
                carryMod,
                logNoiseB,
                warmup,
                iterations
        );

        return new BenchmarkConfig(
                n,
                msgMod,
                carryMod,
                logNoiseB,
                warmup,
                iterations,
                mode
        );
    }

    private int getIntValue(
            TextInputEditText editText,
            String name
    ) {
        if (editText.getText() == null) {
            throw new IllegalArgumentException(name + " is empty");
        }
        String value =
                editText.getText()
                        .toString()
                        .trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException(name + " is empty");
        }
        return Integer.parseInt(value);
    }

    private void validateConfig(
            int n,
            int msgMod,
            int carryMod,
            int logNoiseB,
            int warmup,
            int iterations
    ) {

        if (n <= 0) {
            throw new IllegalArgumentException("n must be > 0");
        }
        if (msgMod <= 0) {
            throw new IllegalArgumentException("msgMod must be > 0");
        }
        if (carryMod <= 0) {
            throw new IllegalArgumentException("carryMod must be > 0");
        }
        if (logNoiseB < 0) {
            throw new IllegalArgumentException(
                    "logNoiseB must be >= 0"
            );
        }
        if (warmup < 0) {
            throw new IllegalArgumentException(
                    "warmup must be >= 0"
            );
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException(
                    "iterations must be > 0"
            );
        }
    }

    private void setBenchmarkRunning(boolean running) {

        btnRunBenchmark.setEnabled(!running);
        btnReset.setEnabled(!running);

        etN.setEnabled(!running);
        etMsgMod.setEnabled(!running);
        etCarryMod.setEnabled(!running);
        etLogNoiseB.setEnabled(!running);
        etWarmup.setEnabled(!running);
        etIterations.setEnabled(!running);
        rgBenchmarkMode.setEnabled(!running);

        for (int i = 0; i < rgBenchmarkMode.getChildCount(); i++) {
            rgBenchmarkMode
                    .getChildAt(i)
                    .setEnabled(!running);
        }
    }

    private void reset() {
        etN.setText("256");
        etMsgMod.setText("2");
        etCarryMod.setText("2");
        etLogNoiseB.setText("46");
        etWarmup.setText("10");
        etIterations.setText("10");
        rgBenchmarkMode.check(R.id.rb_full);
        tvResult.setText("No benchmark result.");
    }

    private String formatConfig(
            BenchmarkConfig config
    ) {
        return String.format(
                Locale.US,
                "Configuration\n" +
                        "------------------------\n" +
                        "n           : %d\n" +
                        "msgMod      : %d\n" +
                        "carryMod    : %d\n" +
                        "logNoiseB   : %d\n" +
                        "warmup      : %d\n" +
                        "iterations  : %d\n" +
                        "mode        : %s\n",

                config.getN(),
                config.getMsgMod(),
                config.getCarryMod(),
                config.getLogNoiseB(),
                config.getWarmupIterations(),
                config.getBenchmarkIterations(),
                config.getMode()
        );
    }

    private String formatResult(
            BenchmarkConfig config,
            BenchmarkResult result
    ) {

        return String.format(
                Locale.US,

                "BENCHMARK RESULT\n" +
                        "================================\n\n" +
                        "Configuration\n" +
                        "--------------------------------\n" +
                        "n           : %d\n" +
                        "msgMod      : %d\n" +
                        "carryMod    : %d\n" +
                        "logNoiseB   : %d\n" +
                        "warmup      : %d\n" +
                        "iterations  : %d\n" +
                        "mode        : %s\n\n" +
                        "Correctness\n" +
                        "--------------------------------\n" +
                        "Status      : %s\n\n" +
                        "Performance\n" +
                        "--------------------------------\n" +
                        "Average     : %.3f ms\n" +
                        "P50         : %.3f ms\n" +
                        "P95         : %.3f ms\n" +
                        "Min         : %.3f ms\n" +
                        "Max         : %.3f ms\n\n" +
                        "Total       : %.3f ms\n",

                config.getN(),
                config.getMsgMod(),
                config.getCarryMod(),
                config.getLogNoiseB(),
                config.getWarmupIterations(),
                config.getBenchmarkIterations(),
                config.getMode(),
                result.isCorrectnessPassed()
                        ? "PASS"
                        : "FAIL",
                nsToMs(result.getAvgNs()),
                nsToMs(result.getP50Ns()),
                nsToMs(result.getP95Ns()),
                nsToMs(result.getMinNs()),
                nsToMs(result.getMaxNs()),
                nsToMs(result.getTotalNs())
        );
    }

    private double nsToMs(double ns) {
        return ns / 1_000_000.0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        benchmarkExecutor.shutdownNow();
    }


}
