package com.limelight.iperf3.cmd;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.limelight.utils.DevUtils;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author shenyong
 * @date 2020-11-10
 */
public class Iperf3Cmd {

    private Context context;
    private CmdCallback callback;

    public Iperf3Cmd(Context context, CmdCallback callback) {
        this.context = context;
        this.callback = callback;
        init();
    }

    private String EXECUTEABLE_NAME = "iperf3.17.1";

    private Pattern CONNECTING_PATTERN = Pattern.compile("(Connecting to host (.*), port (\\d+))");
    private Pattern CONNECTED_PATTERN = Pattern.compile("(local (.*) port (\\d+) connected to (.*) port (\\d+))");
    private Pattern REPORT_PATTERN = Pattern.compile("(\\d{1,2}.\\d{2})-(\\d{1,2}.\\d{2})\\s+sec" +
            "\\s+(\\d+(.\\d+)? [KMGT]?Bytes)\\s+(\\d+(.\\d+)? Mbits/sec)");
    private Pattern UDP_LOSS = Pattern.compile("\\d+/\\d+ \\([\\d+-.e]+%\\)");
    private Pattern TITLE_PATTERN = Pattern.compile("\\[\\s+ID\\]\\s+Interval\\s+Transfer\\s+Bandwidth");
    private Pattern ERR_PATTERN = Pattern.compile("iperf3: error");

    private int parallels = 0;
    private boolean isDown = false;
    // title出现次数。title栏输出第一次之后的、第二次之前的，是中间结果，第二次之后的是最终平均速率
    private int titleCnt = 0;

    private String getAbi() {
        return DevUtils.getCpuApi();
    }

    private String getCmdPath() {
        return context.getFilesDir().getAbsolutePath() + "/" + EXECUTEABLE_NAME;
    }

    private void init() {
        File cmdFile = new File(getCmdPath());
        if (cmdFile.exists()) {
            cmdFile.setExecutable(true, true);
            return;
        }
        try {
            FileUtils.copyInputStreamToFile(context.getAssets().open("iperf3/" + getAbi() + "/" + EXECUTEABLE_NAME), cmdFile);
            cmdFile.setExecutable(true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exec(String[] args) {
        new Thread(() -> {
            String[] cmdAndArgs = new String[args.length + 3];
            cmdAndArgs[0] = getCmdPath();
            cmdAndArgs[cmdAndArgs.length - 2] = "--tmp-template";
            cmdAndArgs[cmdAndArgs.length - 1] = context.getCacheDir().getAbsolutePath() + "/iperf3.XXXXXX";
            System.arraycopy(args, 0, cmdAndArgs, 1, args.length);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d("iperf3", "exec command: " + Arrays.stream(cmdAndArgs).collect(Collectors.joining(" ")));
//            }

            try {
                Process process = Runtime.getRuntime().exec(cmdAndArgs);
                parseArgs(cmdAndArgs);

                try (
                        InputStreamReader in = new InputStreamReader(process.getInputStream());
                        BufferedReader outReader = new BufferedReader(in);
//                        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                ) {
                    String line;
                    while ((line = outReader.readLine()) != null) {
                        parseToCallback(line);
                    }
                    Log.i("CMD", "exitValue: " + process.waitFor());
                }

            } catch (Exception e) {
                Log.e("CMD", e.getMessage(), e);
            }
        }).start();
    }

    private void parseArgs(final String[] cmdAndArgs) {
        isDown = false;
        titleCnt = 0;
        for (int i = 0; i < cmdAndArgs.length; i++) {
            String s = cmdAndArgs[i];
            if ("-P".equals(s)) {
                parallels = Integer.parseInt(cmdAndArgs[i + 1]);
            } else if ("-R".equals(s)) {
                isDown = true;
            }
        }
    }

    private void parseToCallback(String line) {
        Log.d("CMD", line);
//        callback.onRawOutput(line);
//        if (TITLE_PATTERN.matcher(line).find()) {
//            titleCnt++;
//        }
//        Matcher mr = CONNECTING_PATTERN.matcher(line);
//        if (mr.find()) {
//            String addr = mr.group(2);
//            int port = Integer.parseInt(mr.group(3));
//            callback.onConnecting(addr, port);
//        }
//        mr = CONNECTED_PATTERN.matcher(line);
//        if (mr.find()) {
//            String laddr = mr.group(2);
//            int lport = Integer.parseInt(mr.group(3));
//            String raddr = mr.group(4);
//            int rport = Integer.parseInt(mr.group(5));
//            callback.onConnected(laddr, lport, raddr, rport);
//        }
//        // 并发连接数为1和>1时，速率报告有以下两种格式，通过正则捕获组来截取数据
//        // [  4]   9.00-10.00  sec  2.18 MBytes  18.3 Mbits/sec
//        // [SUM]   9.00-10.00  sec  1.85 MBytes  15.5 Mbits/sec
//        mr = REPORT_PATTERN.matcher(line);
//        if (mr.find()) {
//            float st = Float.parseFloat(mr.group(1));
//            float et = Float.parseFloat(mr.group(2));
//            String trans = mr.group(3);
//            String bw = mr.group(5);
//            if (isInterval(line)) {
//                callback.onInterval(st, et, trans, bw, isDown);
//            } else if (isResult(line)) {
//                callback.onResult(st, et, trans, bw, isDown);
//            }
//        }
//        if (ERR_PATTERN.matcher(line).find()) {
//            callback.onError(line);
//        }
    }

    private boolean isInterval(String line) {
        return isTcpInterval(line) || isUdpInterval(line);
    }

    private boolean isResult(String line) {
        return isTcpResult(line) || isUdpResult(line);
    }

    private boolean isTcpInterval(String line) {
        return titleCnt == 1
                && ((parallels == 1)
                || (parallels > 1 && line.startsWith("[SUM]")));
    }

    private boolean isTcpResult(String line) {
        //eg:
        //[ ID] Interval           Transfer     Bandwidth       Retr
        //[  4]   0.00-10.00  sec  19.5 MBytes  16.3 Mbits/sec   19             sender
        //[  4]   0.00-10.00  sec  19.0 MBytes  15.9 Mbits/sec                  receiver
        boolean isLocalResult = (isDown && line.contains("receiver"))
                || (!isDown && line.contains("sender"));
        return titleCnt > 1 && isLocalResult
                && ((parallels == 1)
                || (parallels > 1 && line.startsWith("[SUM]")));
    }

    private boolean isUdpInterval(String line) {
        // parallels == 1 eg:
        //[ ID] Interval           Transfer     Bandwidth       Total Datagrams
        //[  4]   0.00-1.00   sec  9.86 MBytes  82.7 Mbits/sec  1262
        // parallels > 1 eg:
        //[ ID] Interval           Transfer     Bandwidth       Total Datagrams
        //[SUM]   0.00-1.00   sec  10.6 MBytes  88.5 Mbits/sec  1352
        boolean isUdpUpInterval = (titleCnt == 1 && !UDP_LOSS.matcher(line).find())
                && ((parallels == 1)
                || (parallels > 1 && line.startsWith("[SUM]")));
        // parallels == 1 eg:
        //[ ID] Interval           Transfer     Bandwidth       Jitter    Lost/Total Datagrams
        //[  4]   0.00-1.00   sec   240 KBytes  1.96 Mbits/sec  2594.444 ms  12595/12625 (1e+02%)
        // parallels > 1 eg:
        //[ ID] Interval           Transfer     Bandwidth       Jitter    Lost/Total Datagrams
        //[SUM]   0.00-1.00   sec   264 KBytes  2.16 Mbits/sec  6458.650 ms  25698/25731 (1e+02%)
        boolean inUdpDownInterval = (titleCnt == 1 && UDP_LOSS.matcher(line).find())
                && ((parallels == 1)
                || (parallels > 1 && line.startsWith("[SUM]")));
        return isUdpUpInterval || inUdpDownInterval;
    }

    private boolean isUdpResult(String line) {
        //-------- up --------
        // parallels == 1 eg:
        //[ ID] Interval           Transfer     Bandwidth       Jitter    Lost/Total Datagrams
        //[  4]   0.00-10.00  sec  95.3 MBytes  80.0 Mbits/sec  2.963 ms  11168/12029 (93%)
        // parallels > 1 eg:
        //[ ID] Interval           Transfer     Bandwidth       Jitter    Lost/Total Datagrams
        //[SUM]   0.00-10.00  sec   104 MBytes  86.9 Mbits/sec  7.764 ms  11935/12613 (95%)
        //-------- down --------
        // parallels == 1 eg:
        //[ ID] Interval           Transfer     Bandwidth       Jitter    Lost/Total Datagrams
        //[  4]   0.00-10.00  sec  1.16 GBytes   996 Mbits/sec  4.518 ms  151121/151406 (1e+02%)
        // parallels > 1 eg:
        //[ ID] Interval           Transfer     Bandwidth       Jitter    Lost/Total Datagrams
        //[SUM]   0.00-10.00  sec  2.33 GBytes  2002 Mbits/sec  55.278 ms  299008/299247 (1e+02%)
        return (titleCnt > 1 && UDP_LOSS.matcher(line).find())
                && ((parallels == 1)
                || (parallels > 1 && line.startsWith("[SUM]")));
    }
}