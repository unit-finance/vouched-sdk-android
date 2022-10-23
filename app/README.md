## Unit Vouched SDK Sample App

### Introduction
Customers of Unit can use this app to more easily implement the Vouched SDK. This project served as the source for all the examples used in the [Vocuhed SDK for Android](../README.md).
The project includes all of the code, in great detail, with many comments.

### Files to notice

#### UNVouchedService.kt
A service that uses Vouched SDK and intends to show the detection flow in a clear way. It holds `Listener` that is used to pass data to the `MainActivity`.

#### MainActivity.kt
Handles UI changes and holds a reference to a `UNVouchedService`. The activity demonstrates the following flow:
   - Capture an id
   - Capture a selfie(face image)
   - Print the result of the captures that was returned from Vouched.
   
It includes several UI elements such as a confirmation for image view, a manual capture button, a view that indicates certain time had passed and more.

#### DescriptiveTextExt.kt
An extension for Vouched Insight and Instruction enums. It shows a way to control the messages the user will receive on the different scenarios.

### Getting Started
- Clone the [vouched-sdk-android](https://github.com/unit-finance/vocuhed-sdk-android).
- Open the `UNVouchedService` file and fill the `session` variable with a Vouched Public Key as a parameter.
- ![image](https://user-images.githubusercontent.com/108212913/187401168-100e7ae9-e17d-4c07-be4f-2109c31eac4c.png)
- Run
