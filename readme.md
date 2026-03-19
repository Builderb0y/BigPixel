Welcome to Big Pixel: the image editor I made because GIMP, while powerful, does not really do the things that I actually need. And also it's quite tedious to use.

# Compiling

You will need JDK 25 to compile and run Big Pixel. Get it from here: https://adoptium.net/temurin/releases?version=25&os=any&arch=any.

Simply run one of the following commands based on your OS:  
Windows: `gradlew.bat buildAndGenerateLaunchScript`  
Linux: `./gradlew.sh buildAndGenerateLaunchScript`  
Mac: *probably* the same command as linux.

You may need to point your JAVA_HOME environment variable at your java 25 installation directory if the installer doesn't set your PATH variables correctly.

If all goes well, this should create either `BigPixel.bat` (windows) or `BigPixel.sh` (mac/linux). Executing this file should launch Big Pixel.

# Using

todo: write some documentation on how to use this program.