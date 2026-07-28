@rem Copyright 2015 the original author or authors.
@rem Licensed under the Apache License, Version 2.0.
@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem Gradle startup script for Windows
@rem ##########################################################################
@setlocal
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%~n0" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
@endlocal
