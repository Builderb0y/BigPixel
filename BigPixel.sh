#!/usr/bin/env bash

cd /home/builderb0y/Documents/Projects/BigPixel
/home/builderb0y/java/jdk-26-valhalla/bin/java \
-Dbuilderb0y.bigpixel.openQueue=/home/builderb0y/Documents/Projects/BigPixel/open_queue \
--enable-preview \
--add-modules jdk.incubator.vector,javafx.base,javafx.controls,javafx.graphics,org.controlsfx.controls \
--module-path /home/builderb0y/Documents/Projects/BigPixel/dependencies \
--add-exports javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
--add-exports javafx.base/com.sun.javafx.binding=ALL-UNNAMED \
--enable-native-access=javafx.graphics \
-classpath /home/builderb0y/Documents/Projects/BigPixel/out/production/classes:/home/builderb0y/Documents/Projects/BigPixel/out/production/resources:/home/builderb0y/Documents/Projects/BigPixel/dependencies/* \
builderb0y.bigpixel.BigPixel "$@"