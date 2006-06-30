@echo off
set DATABEANS_HOME=%~dp0..
java -classpath "%DATABEANS_HOME%"\lib\bsh.jar;"%DATABEANS_HOME%"\lib\databeans_admin.jar -Djava.security.policy="%DATABEANS_HOME%"\lib\security\databeans.policy bsh.Interpreter %1
