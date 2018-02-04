/**
 * Copyright (c) 2017 Liu Dongmiao
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.piebridge;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by thom on 2017/11/15.
 */
public class SimpleSu {

    private static final String TAG = "SimpleSu";

    private static final Object LOCK = new Object();

    private static final String BOM = UUID.randomUUID().toString();

    private static final String EOF = UUID.randomUUID().toString();

    private static boolean exit;

    private static Runnable kill;

    private SimpleSu() {

    }

    public static void setKill(Runnable runnable) {
        kill = runnable;
    }

    public static void su(String command) {
        su(command, false);
    }

    public static String su(String command, boolean output) {
        synchronized (LOCK) {
            return exec(command, output);
        }
    }

    static void kill() {
        if (kill != null) {
            kill.run();
        }
    }

    private static String exec(String command, boolean output) {
        i(">>> +++++++");
        i("[suexec] " + command);
        StringWriter sw = new StringWriter();
        try {
            exit = false;
            Process process = Runtime.getRuntime().exec("su");
            Thread out = null;
            Thread err = null;
            if (output) {
                PrintWriter pw = new PrintWriter(sw);
                out = new Thread(new StreamGobbler(process.getInputStream(), pw, "[stdout] "));
                err = new Thread(new StreamGobbler(process.getErrorStream(), pw, "[stderr] "));
                out.start();
                err.start();
            }
            PrintWriter stdin = new PrintWriter(process.getOutputStream());
            stdin.println("echo " + BOM);
            stdin.println("echo " + BOM + " >&2");
            stdin.println(command);
            stdin.println("echo " + EOF);
            stdin.println("exit");
            stdin.flush();
            stdin.close();
            process.waitFor();
            exit = true;
            if (output) {
                out.join();
                err.join();
            }
            return sw.toString();
        } catch (IOException e) {
            w("[suwarn] io exception", e);
            return null;
        } catch (InterruptedException e) {
            w("[suwarn] interrupted", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            i("+++++++ <<<");
        }
    }

    public static boolean hasSu() {
        return Holder.SU;
    }

    static void i(String msg) {
        Log.i(TAG, msg);
    }

    static void i(String msg, Throwable t) {
        Log.i(TAG, msg, t);
    }

    static void w(String msg, Throwable t) {
        Log.w(TAG, msg, t);
    }

    private static class Holder {
        static final boolean SU = checkSu();

        private Holder() {

        }

        private static boolean checkSu() {
            for (String dir : System.getenv("PATH").split(":")) {
                File path = new File(dir, "su");
                try {
                    if (Os.access(path.getPath(), 1)) {
                        i("has su: " + path);
                        return true;
                    }
                } catch (ErrnoException e) {
                    i("Can't access " + path);
                }
            }
            i("has no su");
            return false;
        }
    }

    private static class StreamGobbler implements Runnable {
        private final InputStream is;
        private final PrintWriter pw;
        private final String prefix;
        private volatile boolean bom;

        private final ScheduledExecutorService executor;
        private Future future;

        StreamGobbler(InputStream is, PrintWriter pw, String prefix) {
            this.is = new BufferedInputStream(is);
            this.pw = pw;
            this.prefix = prefix;
            this.executor = new ScheduledThreadPoolExecutor(0x1);
        }

        @Override
        public void run() {
            try (
                    BufferedReader br = new BufferedReader(new InputStreamReader(is))
            ) {
                String line;
                boolean shouldBreak = false;
                while (true) {
                    if (br.ready()) {
                        line = br.readLine();
                        shouldBreak = line == null || onLine(line);
                    } else if (shouldBreak || exit || sleep(0x1)) {
                        break;
                    }
                }
            } catch (IOException e) {
                i(prefix + "io exception", e);
            }
        }

        private boolean sleep(int i) {
            try {
                TimeUnit.SECONDS.sleep(i);
                return false;
            } catch (InterruptedException e) { // NOSONAR
                Thread.currentThread().interrupt();
                i(prefix + "read interrupted");
                return true;
            }
        }

        private boolean onLine(String line) {
            if (line.endsWith(EOF)) {
                if (!EOF.equals(line)) {
                    write(line.substring(0, line.length() - EOF.length()));
                }
                scheduleKill();
                return true;
            }

            if (bom) {
                write(line);
            } else if (BOM.equals(line)) {
                bom = true;
                cancelKill();
            } else {
                write(line);
                scheduleKill();
            }
            return false;
        }

        private void cancelKill() {
            if (future != null) {
                future.cancel(true);
            }
        }

        private void scheduleKill() {
            if (future != null) {
                future.cancel(true);
            }
            future = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    kill();
                }
            }, 0x3, TimeUnit.SECONDS);
        }

        private void write(String line) {
            i(prefix + line);
            synchronized (pw) {
                pw.println(line);
                pw.flush();
            }
        }
    }

}
