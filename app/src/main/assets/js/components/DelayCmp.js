"use strict";

const DelayCmp = ({
                      actualDelay, initialDelay, delay, delayOnChange,
                      coefs, coefsOnChange,
                      delayTextFieldRef, delayTextFieldId, delayTextFieldTabIndex, updateDelayRequestIsInProgress,
                      onSubmit, onF9}) => {
    const {renderMessagePopup, showDialog} = useMessagePopup()

    function renderChangeInfo() {
        return RE.Container.row.left.center({},{style:{marginRight:'20px', fontSize: '15px', color:'gray'}},
            RE.span({style:{}}, `[${actualDelay}]`),
            RE.span({style:{}}, initialDelay),
            RE.span({style:{}}, '\u{02192}'),
            RE.span({style:{fontSize: '15px', fontFamily:'monospace', fontWeight:'bold'}}, delay),
        )
    }

    async function openSettings() {
        const newCoefs = await showDialog({
            title: 'Delay coefficients:',
            contentRenderer: resolve => re(DelayCoefsSettingsCmp,{
                coefs,
                onOk: newCoefs => resolve(newCoefs),
                onCancel: () => resolve(null)
            })
        })
        if (hasValue(newCoefs)) {
            coefsOnChange(newCoefs)
        }
    }

    function toggleCoef(idx) {
        const coef = coefs[idx]
        delay !== coef ? delayOnChange(coef) : delayOnChange(initialDelay)
    }

    function renderCoefs() {
        return re(KeyPad, {
            componentKey: "controlButtons",
            buttonWidth:'55px',
            keys: [[
                ...coefs.map((coef,idx) => ({
                    onClick: () => {
                        if (coef !== '') {
                            toggleCoef(idx)
                        }
                    },
                    style:{backgroundColor: coef !== '' && delay === coef ? '#00d0ff' : undefined},
                    symbol:coef,
                })),
                {
                    onClick: openSettings,
                    style:{},
                    iconName:'settings',
                }
            ]],
            variant: "outlined",
        })
    }

    function delayFormOnKeyDown(event) {
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
        } else if (keyCode === F9_KEY_CODE) {
            event.preventDefault();
            onF9?.()
        }
        if (hasValue(idx) && coefs[idx] !== '') {
            toggleCoef(idx)
        }
    }

    return RE.Container.col.top.left({},{style:{marginBottom:'15px'}},
        renderChangeInfo(),
        renderCoefs(),
        re(DelayForm, {
            initialDelay,
            delay:delay.charAt(0) === 'x' ? initialDelay : delay,
            delayOnChange,
            delayTextFieldRef, delayTextFieldId, delayTextFieldTabIndex, updateDelayRequestIsInProgress,
            onSubmit,
            onKeyDown: delayFormOnKeyDown,
            result:delay,
        }),
        renderMessagePopup()
    )
}
