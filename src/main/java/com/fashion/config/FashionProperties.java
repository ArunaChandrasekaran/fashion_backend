package com.fashion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "fashion")
public class FashionProperties {

    private String frontendOrigin = "http://localhost:5173,http://localhost:5500,http://127.0.0.1:5500,http://localhost:5501,http://127.0.0.1:5501,http://localhost:5502,http://127.0.0.1:5502";
    private Admin admin = new Admin();
    private boolean seedDemo;
    private String uploadDir = "uploads";
    private Shipping shipping = new Shipping();
    private int lowStockThreshold = 5;

    public String getFrontendOrigin() {
        return frontendOrigin;
    }

    public void setFrontendOrigin(String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    public List<String> allowedOrigins() {
        return Arrays.stream(frontendOrigin.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public boolean isSeedDemo() {
        return seedDemo;
    }

    public void setSeedDemo(boolean seedDemo) {
        this.seedDemo = seedDemo;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public Shipping getShipping() {
        return shipping;
    }

    public void setShipping(Shipping shipping) {
        this.shipping = shipping;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public static class Admin {
        private String email = "";
        private String password = "";

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Shipping {
        private BigDecimal threshold = new BigDecimal("2499.00");
        private BigDecimal fee = new BigDecimal("99.00");

        public BigDecimal getThreshold() {
            return threshold;
        }

        public void setThreshold(BigDecimal threshold) {
            this.threshold = threshold;
        }

        public BigDecimal getFee() {
            return fee;
        }

        public void setFee(BigDecimal fee) {
            this.fee = fee;
        }
    }
}
