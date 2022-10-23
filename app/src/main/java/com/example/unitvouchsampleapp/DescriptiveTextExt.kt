package com.example.unitvouchsampleapp

import id.vouched.android.Instruction
import id.vouched.android.VouchedCameraHelper
import id.vouched.android.model.Insight

/**
 * This class is responsible for most of the messages that will be seen on screen by the user;
 *
 * Extensions for the `Insight` and `Instruction` String Enums
 */
fun Instruction.getDescriptiveText(mode: VouchedCameraHelper.Mode) = when (this) {
    Instruction.NO_CARD -> "No card"
    Instruction.ROTATE_TO_HORIZONTAL -> "Rotate to horizontal"
    Instruction.ROTATE_TO_VERTICAL -> "Rotate to vertical"
    Instruction.GLARE -> "Glare"
    Instruction.DARK -> "Dark"
    Instruction.BLUR -> "Blur"
    Instruction.HOLD_STEADY -> "Hold Steady"
    Instruction.NO_FACE -> "Show Face"
    Instruction.OPEN_MOUTH -> "Open Mouth"
    Instruction.CLOSE_MOUTH -> "Close Mouth"
    Instruction.LOOK_FORWARD -> "Look Forward"
    Instruction.LOOK_LEFT -> "Look Left"
    Instruction.LOOK_RIGHT -> "Look Right"
    Instruction.BLINK_EYES -> "Blink Eyes"
    Instruction.ONLY_ONE -> when (mode) {
        VouchedCameraHelper.Mode.ID -> "Multiple IDs"
        VouchedCameraHelper.Mode.FACE -> "Multiple Faces"
        else -> "Detecting Multiple Items"
    }
    Instruction.MOVE_CLOSER -> when (mode) {
        VouchedCameraHelper.Mode.FACE -> "Come Closer to the Camera"
        else -> "Move Closer"
    }
    Instruction.MOVE_AWAY -> when (mode) {
        VouchedCameraHelper.Mode.ID -> "Move ID away"
        else -> "Move away"
    }
    else -> null
}

fun Insight.getDescriptiveText() = when (this) {
    Insight.NON_GLARE -> "Image has glare"
    Insight.QUALITY -> "Image is blurry"
    Insight.BRIGHTNESS -> "Image needs to be brighter"
    Insight.FACE -> "Image is missing required visual markers"
    Insight.GLASSES -> "Please take off your glasses"
    Insight.ID_PHOTO -> "ID needs a valid photo"
    else -> "Unknown Error"
}
