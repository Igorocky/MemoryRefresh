'use strict';

function createBeFunction(funcName) {
    return async dto => {
        const res = await fetch(`/be/${funcName}`, {
            method: 'POST',
            body: JSON.stringify(dto??{}),
        })
        return res.json()
    }
}

