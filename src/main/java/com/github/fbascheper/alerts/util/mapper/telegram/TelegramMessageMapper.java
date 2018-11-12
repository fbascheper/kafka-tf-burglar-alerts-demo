package com.github.fbascheper.alerts.util.mapper.telegram;

import com.github.fbascheper.alerts.model.avro.SerializableImage;
import com.github.fbascheper.kafka.connect.telegram.*;

/**
 * Telegram message mapper.
 *
 * @author Erik-Berndt Scheper
 * @since 02-11-2018
 */
public final class TelegramMessageMapper {

    private TelegramMessageMapper() {
        // prevent instantiation
    }

    /**
     * Build a text based Telegram message.
     *
     * @param message the text to send
     * @return a Telegram message
     */
    public static TgMessage textMessage(String message) {

        TgTextMessage textMessage = new TgTextMessage();
        textMessage.setText(message);

        TgMessage tgMessage = new TgMessage();
        tgMessage.setMessageType(TgMessageType.TEXT);
        tgMessage.setTextMessage(textMessage);

        return tgMessage;
    }

    /**
     * Build a Telegram message from an image and a caption (text).
     *
     * @param image   the image to include in the message
     * @param caption the caption of the image
     * @return a Telegram message
     */
    public static TgMessage photoMessage(SerializableImage image, String caption) {
        TgAttachment attachment = new TgAttachment();
        attachment.setName(image.getName());
        attachment.setContents(image.getImageData());

        TgPhotoMessage photoMessage = new TgPhotoMessage();
        photoMessage.setCaption(caption);
        photoMessage.setPhoto(attachment);

        TgMessage tgMessage = new TgMessage();
        tgMessage.setMessageType(TgMessageType.PHOTO);
        tgMessage.setPhotoMessage(photoMessage);

        return tgMessage;
    }

}
