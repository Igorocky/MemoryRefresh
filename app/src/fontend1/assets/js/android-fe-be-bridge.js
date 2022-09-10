'use strict';

function createFeBeBridgeForAndroid() {
    const feCallbacks = []
    const feCallbackCnt = {cnt:0}

    function createFeCallback(resultHandler) {
        const id = feCallbackCnt.cnt++
        feCallbacks.push({id,resultHandler})
        return id
    }

    function callFeCallback(cbId,result) {
        const idx = feCallbacks.findIndex(cb => cb.id === cbId)
        if (idx >= 0) {
            removeAtIdx(feCallbacks, idx).resultHandler(result)
        }
    }

    function createBeFunction(functionName) {
        return dto => new Promise((resolve, reject) => {
            BE.invokeBeMethod(createFeCallback(resolve), functionName, JSON.stringify(dto??{}))
        })
    }

    return {
        createBeFunction,
        feBeBridgeState: {feCallbacks, feCallbackCnt},
        callFeCallback
    }
}

const androidFeBeBridge = createFeBeBridgeForAndroid()

function callFeCallback(cbId,result) {
    androidFeBeBridge.callFeCallback(cbId,result)
}

const createBeFunction = androidFeBeBridge.createBeFunction