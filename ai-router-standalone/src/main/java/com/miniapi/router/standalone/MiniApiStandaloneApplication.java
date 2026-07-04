package com.miniapi.router.standalone;

import com.miniapi.router.standalone.config.SetupWizard;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication(scanBasePackages = {"com.miniapi.router.standalone", "com.miniapi.router.core"})
@MapperScan("com.miniapi.router.standalone.mapper")
@EnableAsync
public class MiniApiStandaloneApplication {

    public static void main(String[] args) {
        try {
            ensureDataDirectories();
            SetupWizard.runIfFirstTime(args);
            SpringApplication.run(MiniApiStandaloneApplication.class, args);
        } catch (Throwable t) {
            System.err.println("[Fatal] MiniAPIRouter Standalone failed to start: " + t.getMessage());
            t.printStackTrace(System.err);
            pauseOnWindowsConsole();
            throw t;
        }
    }

    private static void ensureDataDirectories() {
        try {
            Path baseDir = SetupWizard.getBaseDir();
            Files.createDirectories(baseDir);
            Files.createDirectories(baseDir.resolve("logs"));
            System.setProperty("miniapi.router.data-dir", baseDir.toString());
        } catch (Exception e) {
            System.err.println("[Init] Failed to create data directories: " + e.getMessage());
        }
    }

    private static void pauseOnWindowsConsole() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("win") || System.console() == null) {
            return;
        }

        System.err.println();
        System.err.println("Press Enter to exit...");
        try {
            System.in.read();
        } catch (Exception ignored) {
        }
    }
}
