## Demo application - using TensorFlow and Apache Kafka for a burglar alerts application

> This project uses a TensorFlow model created by the [TensorFlow Burglar alert model sub project](https://github.com/fbascheper/kafka-tf-burglar-alerts-demo-model)

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/fbascheper/kafka-tf-burglar-alerts-demo/master/LICENSE.txt)

***

### Summary
This repository contains a [Kafka Streams](https://kafka.apache.org/documentation/streams) 
application that can process webcam images uploaded into Apache Kafka. It uses 
[TensorFlow for Java](https://www.tensorflow.org/install/lang_java) to detect potential burglars and 
will send burglar alerts using the [Telegram connector](https://www.confluent.io/connector/kafka-connect-telegram/) for Kafka Connect.

However, not all detected burglar alerts are sent to the Telegram connector. Depending on the state of a 
[Nuki smartlock](https://nuki.io/en/), alerts are filtered and discarded. The state of the smartlock is
retrieved using the [Nuki Web API](https://developer.nuki.io/t/nuki-web-api) and loaded into Kafka using the
[REST Connector](https://github.com/llofberg/kafka-connect-rest) for Kafka Connect.

The idea behind this is that sending burglar alerts only makes sense when you're either not at home or (hopefully) 
fast asleep. And those situations tend to coincide with a given lock state (locked).


### WebCam image processing
Before the images can be processed by the Kafka Streams application, they must
be uploaded into Kafka. When your camera supports FTP, such as those from [Foscam](https://www.foscam.com), you
can use the [Landoop Kafka Connect FTP converter](https://github.com/Landoop/stream-reactor/commits/master/kafka-connect-ftp)
to achieve this.

However, there are a few important issues to consider, to facilitate the image detection by TensorFlow for Java.
1. Pictures taken with sufficient light will be in full colour, but those taken at night are made with infrared
   light and thus in grayscale. Therefore it's better to convert all images to grayscale.
2. Full HD cameras have resolutions that lead to impractical image sizes. It's better to scale the image down to 
   a smaller size, such as a width of 1024. That will also help to prevent exceeding the maximum message size in Kafka. 
3. If your camera records areas that are irrelevant for burglar detection, 
   such as the street in front of your house, you can crop parts of the image.
   This may also help preventing false alerts to be sent by the application.    

These issues can be handled by a custom [Kafka Connect value converter](https://github.com/fbascheper/kafka-connect-storage-converters).
You can simply plug this into the [Kafka Connect FTP converter](https://github.com/Landoop/stream-reactor/commits/master/kafka-connect-ftp).


### Hardware used
- Synology NAS - with [Docker feature](https://www.synology.com/nl-nl/dsm/feature/docker) supported
- Outdoor camera that supports motion detection and FTPS upload, e.g [Foscam FI9901 EP](https://foscam.com/products/Bullet_camera.html)
- Nuki smart locks  

### Software components

- Kafka Streams
- Kafka Connect
  - FTP connector, used to load images into Kafka
  - REST connector, used to retrieve the smartlock state
  - Telegram connector, used to send alerts from Kafka
- Kafka Connect value converter, to crop, resize and convert images to grayscale
- TensorFlow for Java

