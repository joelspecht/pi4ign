# Raspberry Pi Module Example 

This is an example module which adds support for Raspberry Pi. This module uses the experimental [Pi4j v2 library](https://github.com/Pi4J/pi4j-v2) and currently only supports Raspberry Pi 4 Model B. It is recommended not to use this module for production purposes at this time - its purpose is currently limited to a Proof-of-Concept.

## How to Build

Use the gradle wrapper to build the module:

*nix: `gradlew clean buildModule`

Windows: `gradlew.bat clean buildModule`

The unsigned module build will be created at `build/pi4ign-unsigned.modl`

## Installing the Module

The module must be installed on version 8.1 Ignition Gateways. The `pigpio` providers should be used when configuring a new Pi4BDevice instance in the OPC Device Connections Gateway Web Interface Config Page. Since `pigpio` must be run as root, so must the Gateway, otherwise failures will result.

If you wish to install the unsigned module, you must have the following system property set in your `ignition.conf` file:

`wrapper.java.additional.5=-Dignition.allowunsignedmodules=true`

Otherwise, you will need to use the [module signer tool](https://github.com/inductiveautomation/module-signer) to sign the module before installing it.

## Installing Pi4J Plugins

To install a Pi4J plugin, copy all required jar files to `$IGNITION_HOME/data/modules/com.joelspecht.pi4ign/plugins/*`. For example: the pigpio plugin requires two jar files: (1) `pi4j-plugin-pigpio-2.0-SNAPSHOT.jar` and (2) `pi4j-library-pigpio-2.0-SNAPSHOT.jar`
