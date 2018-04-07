setup Opencv android module library

this is the setup flow.

1. createn an android empty project. it is the 1st commit.
2. 添加opencv源码：File -> new -> import module -> select opencvsdk/sdk/java. 
添加App工程对opencv库的依赖：File->Project Structure->选中app的Dependencies选项，点击”+”选中Module dependencies
it is the 2nd commit.
3. 添加Opencv的lib库到AndroidStudio里的JniLib中。
it is the 3rd commit.
4. 通过OpenCV库实现Camera渲染到手机屏幕，以便验证OpenCV库移植到Android Studio是否成功
it is the 4rd commit.