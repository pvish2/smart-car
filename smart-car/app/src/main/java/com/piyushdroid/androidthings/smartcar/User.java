package com.piyushdroid.androidthings.smartcar;

public enum User {



        DRIVER("Driver"),
        EMERGENCY("Emergency");

        private String user;

    User(String user) {
            this.user = user;
        }

        public String getUser() {
            return user;
        }

}
