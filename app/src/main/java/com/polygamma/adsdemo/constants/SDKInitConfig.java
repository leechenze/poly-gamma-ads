package com.polygamma.adsdemo.constants;

public class SDKInitConfig {

    // Privacy settings
    public boolean isAdult;
    public boolean isPersonalized;
    public boolean isProgrammatic;

    // Device settings
    public boolean canUseLocation;
    public boolean canUsePhoneState;
    public boolean canUseOaid;
    public boolean canUseAndroidId;
    public boolean canUseAppList;
    public boolean canUseSimOperator;
    public boolean canUseSpaceSize;


    private SDKInitConfig(Builder builder) {
        this.isAdult = builder.isAdult;
        this.isPersonalized = builder.isPersonalized;
        this.isProgrammatic = builder.isProgrammatic;

        this.canUseLocation = builder.canUseLocation;
        this.canUsePhoneState = builder.canUsePhoneState;
        this.canUseOaid = builder.canUseOaid;
        this.canUseAndroidId = builder.canUseAndroidId;
        this.canUseAppList = builder.canUseAppList;
        this.canUseSimOperator = builder.canUseSimOperator;
        this.canUseSpaceSize = builder.canUseSpaceSize;
    }

    public boolean isAdult() {
        return isAdult;
    }

    public boolean isPersonalized() {
        return isPersonalized;
    }

    public boolean isProgrammatic() {
        return isProgrammatic;
    }

    public boolean canUseLocation() {
        return canUseLocation;
    }

    public boolean canUsePhoneState() {
        return canUsePhoneState;
    }

    public boolean canUseOaid() {
        return canUseOaid;
    }

    public boolean canUseAndroidId() {
        return canUseAndroidId;
    }

    public boolean canUseAppList() {
        return canUseAppList;
    }

    public boolean canUseSimOperator() {
        return canUseSimOperator;
    }

    public boolean canUseSpaceSize() {
        return canUseSpaceSize;
    }


    public static class Builder {

        private boolean isAdult = true;
        private boolean isPersonalized = true;
        private boolean isProgrammatic = true;
        private boolean canUseLocation = true;
        private boolean canUsePhoneState = true;
        private boolean canUseOaid = true;
        private boolean canUseAndroidId = true;
        private boolean canUseAppList = true;
        private boolean canUseSimOperator = true;
        private boolean canUseSpaceSize = true;

        public Builder setIsAdult(boolean isAdult) {
            this.isAdult = isAdult;
            return this;
        }

        public Builder setIsPersonalized(boolean isPersonalized) {
            this.isPersonalized = isPersonalized;
            return this;
        }

        public Builder setIsProgrammatic(boolean isProgrammatic) {
            this.isProgrammatic = isProgrammatic;
            return this;
        }

        public Builder setCanUseLocation(boolean canUseLocation) {
            this.canUseLocation = canUseLocation;
            return this;
        }

        public Builder setCanUsePhoneState(boolean canUsePhoneState) {
            this.canUsePhoneState = canUsePhoneState;
            return this;
        }

        public Builder setCanUseOaid(boolean canUseOaid) {
            this.canUseOaid = canUseOaid;
            return this;
        }

        public Builder setCanUseAndroidId(boolean canUseAndroidId) {
            this.canUseAndroidId = canUseAndroidId;
            return this;
        }

        public Builder setCanUseAppList(boolean canUseAppList) {
            this.canUseAppList = canUseAppList;
            return this;
        }

        public Builder setCanUseSimOperator(boolean canUseSimOperator) {
            this.canUseSimOperator = canUseSimOperator;
            return this;
        }

        public Builder setCanUseSpaceSize(boolean canUseSpaceSize) {
            this.canUseSpaceSize = canUseSpaceSize;
            return this;
        }

        public SDKInitConfig build() {
            return new SDKInitConfig(this);
        }
    }
}

