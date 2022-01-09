"use strict";

const FLOATING_NUMBER_REGEX = /^\d+(\.\d*)?$/

const DelayCoefsSettingsCmp = ({coefs, onOk, onCancel}) => {

    const [newCoefs, setNewCoefs] = useState(coefs)

    function renderCoefTextField({value, label, onChange}) {
        return RE.TextField({
            autoCorrect: 'off', autoCapitalize: 'off', spellCheck: 'false',
            value,
            label,
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            size: 'small',
            inputProps: {size:8},
            onChange: event => {
                const newText = event.nativeEvent.target.value.trim()
                if (newText === '' || FLOATING_NUMBER_REGEX.test(newText)) {
                    onChange(newText)
                }
            },
        })
    }

    function getCoefValue(idx) {
        return newCoefs[idx].substring(1)
    }

    function setCoefValue(idx,newValue) {
        setNewCoefs(prev => prev.map((v,i) => i === idx ? (newValue === '' ? '' : 'x' + newValue) : v))
    }

    function renderOkButton() {
        return RE.Button({variant: 'contained', color: 'primary', onClick: () => onOk(newCoefs)}, 'save')
    }

    function renderCancelButton() {
        return RE.Button({onClick: onCancel}, 'cancel')
    }

    return RE.Container.col.top.left({},{style:{marginBottom:'20px'}},
        renderCoefTextField({value:getCoefValue(0), label:'F1', onChange: newValue => setCoefValue(0,newValue)}),
        renderCoefTextField({value:getCoefValue(1), label:'F2', onChange: newValue => setCoefValue(1,newValue)}),
        renderCoefTextField({value:getCoefValue(2), label:'F3', onChange: newValue => setCoefValue(2,newValue)}),
        renderCoefTextField({value:getCoefValue(3), label:'F4', onChange: newValue => setCoefValue(3,newValue)}),
        RE.Container.row.right.center({},{},
            renderCancelButton(),
            renderOkButton()
        )
    )

}
