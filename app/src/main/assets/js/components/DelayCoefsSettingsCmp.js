"use strict";

const DELAY_COEFS_SETTINGS_DELAY_DUR_REGEX = /^\d+[smhdM]$/
const DELAY_COEFS_SETTINGS_DELAY_COEF_REGEX = /^x\d+(\.\d+)?$/
const DELAY_COEFS_SETTINGS_POSSIBLE_DEFAULT_COEFS = ['F1','F2','F3','F4']

const DelayCoefsSettingsCmp = ({coefs, defCoefs, maxDelay, onOk, onCancel}) => {

    const [newCoefs, setNewCoefs] = useState(coefs)
    const [newDefCoefs, setNewDefCoefs] = useState(defCoefs)
    const [newMaxDelay, setNewMaxDelay] = useState(maxDelay)

    function isCoefCorrect(coef) {
        return '' === coef || DELAY_COEFS_SETTINGS_DELAY_DUR_REGEX.test(coef) || DELAY_COEFS_SETTINGS_DELAY_COEF_REGEX.test(coef)
    }

    function isMaxDelayCorrect(maxDelay) {
        return DELAY_COEFS_SETTINGS_DELAY_DUR_REGEX.test(maxDelay)
    }

    function renderCoefTextField({value, label, onChange}) {
        return textField({
            value,
            label,
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            size: 'small',
            color: isCoefCorrect(value) ? 'primary' : 'secondary',
            inputProps: {size:8},
            onChange: event => {
                onChange(event.nativeEvent.target.value.trim())
            },
        })
    }

    function setCoefValue(idx,newValue) {
        setNewCoefs(prev => prev.map((v,i) => i === idx ? newValue : v))
    }

    function renderOkButton() {
        return RE.Button({
            variant: 'contained',
            color: 'primary',
            onClick: () => onOk({newCoefs,newMaxDelay,newDefCoefs}),
            disabled: newCoefs.find(c => !isCoefCorrect(c)) || !isMaxDelayCorrect(newMaxDelay)
        }, 'save')
    }

    function renderCancelButton() {
        return RE.Button({onClick: onCancel}, 'cancel')
    }

    return RE.Container.col.top.left({},{style:{marginBottom:'20px'}},
        renderCoefTextField({value:newCoefs[0], label:'F1', onChange: newValue => setCoefValue(0,newValue)}),
        renderCoefTextField({value:newCoefs[1], label:'F2', onChange: newValue => setCoefValue(1,newValue)}),
        renderCoefTextField({value:newCoefs[2], label:'F3', onChange: newValue => setCoefValue(2,newValue)}),
        renderCoefTextField({value:newCoefs[3], label:'F4', onChange: newValue => setCoefValue(3,newValue)}),
        RE.FormControl({variant:"outlined"},
            RE.InputLabel({id:'oncorrect'}, 'On correct'),
            RE.Select(
                {
                    value:hasValue(newDefCoefs.onCorrect)?newDefCoefs.onCorrect:-1,
                    variant: 'outlined',
                    label: 'On correct',
                    labelId: 'oncorrect',
                    onChange: event => {
                        setNewDefCoefs(prev => ({...prev, onCorrect: event.target.value >= 0 ? event.target.value : null}))
                    }
                },
                ints(-1,3).map(idx => RE.MenuItem({key:idx, value:idx}, idx<0 ? 'None' : DELAY_COEFS_SETTINGS_POSSIBLE_DEFAULT_COEFS[idx]))
            )
        ),
        RE.FormControl({variant:"outlined"},
            RE.InputLabel({id:'onerror'}, 'On error'),
            RE.Select(
                {
                    value:hasValue(newDefCoefs.onError)?newDefCoefs.onError:-1,
                    variant: 'outlined',
                    label: 'On error',
                    labelId: 'onerror',
                    onChange: event => {
                        setNewDefCoefs(prev => ({...prev, onError: event.target.value >= 0 ? event.target.value : null}))
                    }
                },
                ints(-1,3).map(idx => RE.MenuItem({key:idx, value:idx}, idx<0 ? 'None' : DELAY_COEFS_SETTINGS_POSSIBLE_DEFAULT_COEFS[idx]))
            )
        ),
        textField({
            value: newMaxDelay,
            label: 'Max delay',
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            size: 'small',
            color: isMaxDelayCorrect(newMaxDelay) ? 'primary' : 'secondary',
            inputProps: {size: 8},
            onChange: event => {
                setNewMaxDelay(event.nativeEvent.target.value.trim())
            },
        }),
        RE.Container.row.right.center({},{},
            renderCancelButton(),
            renderOkButton()
        )
    )

}
