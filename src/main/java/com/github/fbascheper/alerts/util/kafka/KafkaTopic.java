package com.github.fbascheper.alerts.util.kafka;

/**
 * Enumeration of all topics used by the burglar alert system.
 *
 * @author Erik-Berndt Scheper
 * @since 05-11-2018
 */
public enum KafkaTopic {

    /**
     * Source topic where all IP-camera images are posted.
     */
    IP_CAMERA_IMAGE("burglar-alerts-camera-ftp-topic"),

    /**
     * Source topic where all Nuki REST api calls are posted.
     */
    NUKI_REST_API_RESULT("burglar-alerts-smartlock-rest-topic"),

    /**
     * Sink topic where all Telegram messages are posted.
     */
    TELEGRAM_BURGLAR_ALERT("burglar-alerts-telegram-topic"),

    /**
     * Topic where the on/off state regarding the sending of alert messages is posted.
     */
    ALERTING_ENABLED_STATE("burglar-alerts-alerting-enabled-state-topic");

    private final String name;

    KafkaTopic(String name) {
        this.name = name;
    }

    /**
     * @return the name of this topic.
     */
    public String getName() {
        return name;
    }

}
