"use strict";

const DELAY_COEFS_SETTINGS_DELAY_DUR_REGEX = /^(\d+)[smhdM]$/
const DELAY_COEFS_SETTINGS_DELAY_COEF_REGEX = /^x\d+(\.\d+)?$/

const DelayCoefsSettingsCmp = ({coefs, onOk, onCancel}) => {

    const [newCoefs, setNewCoefs] = useState(coefs)

    function isCoefCorrect(coef) {
        return '' === coef || DELAY_COEFS_SETTINGS_DELAY_DUR_REGEX.test(coef) || DELAY_COEFS_SETTINGS_DELAY_COEF_REGEX.test(coef)
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
            onClick: () => onOk(newCoefs),
            disabled: newCoefs.find(c => !isCoefCorrect(c))
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
        RE.Container.row.right.center({},{},
            renderCancelButton(),
            renderOkButton()
        )
    )

}
