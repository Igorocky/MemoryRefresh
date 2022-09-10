"use strict";

function getDelayCoefByKeyCode({event, coefs, currDelay, initialDelay}) {
    const keyCode = event.keyCode
    let idx
    if (keyCode === F1_KEY_CODE) {
        event.preventDefault();
        idx = 0
    } else if (keyCode === F2_KEY_CODE) {
        event.preventDefault();
        idx = 1
    } else if (keyCode === F3_KEY_CODE) {
        event.preventDefault();
        idx = 2
    } else if (keyCode === F4_KEY_CODE) {
        event.preventDefault();
        idx = 3
    }
    if (hasValue(idx) && coefs[idx] !== '') {
        const coef = coefs[idx]
        return currDelay !== coef ? coef : initialDelay
    }
}