Welcome to Big Pixel: the image editor I made because GIMP, while powerful, does not really do the things that I actually need. And also it's quite tedious to use.

Big Pixel does what I need it to do, and not much more than that. I make no guarantees that it will be suitable for things that other people want to use it for.

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

# Disclaimer

I make no guarantees that Big Pixel is bug-free. In fact, I know it has bugs in it. Some bugs may even cause loss of work. Big Pixel won't auto-save. And it also won't ask you if you want to save unsaved changes when you close the window. Save your work frequently, and submit an issue if a file you previously saved no longer opens correctly.