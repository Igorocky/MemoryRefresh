"use strict";

const UpdateTranslateCardCmp = ({
                                    textToTranslate,textToTranslateOnChange,textToTranslateBgColor,
                                    translation,translationOnChange,translationBgColor,
                                    delay,delayOnChange,delayBgColor,
                                    onSave, saveDisabled,
                                    onCancel, cancelDisabled,
                                    onDelete,
                                }) => {

    function renderSaveButton() {
        return iconButton({
            iconName: 'save',
            disabled: saveDisabled,
            iconStyle:{color:saveDisabled?'lightgrey':'blue'},
            onClick: onSave
        })
    }

    function renderCancelButton() {
        if (onCancel) {
            return iconButton({
                iconName: 'close',
                disabled: cancelDisabled,
                iconStyle:{color:'black'},
                onClick: onCancel
            })
        }
    }

    function renderDeleteButton() {
        if (onDelete) {
            return iconButton({
                iconName: 'delete',
                iconStyle:{color:'red'},
                onClick: onDelete
            })
        }
    }

    function renderButtons() {
        return RE.Container.row.left.center({}, {style: {}},
            renderDeleteButton(),
            renderCancelButton(),
            renderSaveButton()
        )
    }

    return RE.Container.col.top.left({}, {style: {marginBottom: '5px'}},
        renderButtons(),
        RE.If(hasValue(textToTranslate), () => RE.TextField({
            autoCorrect: 'off', autoCapitalize: 'none', spellCheck: 'false',
            value: textToTranslate,
            label: 'Text to translate',
            variant: 'outlined',
            autoFocus: true,
            multiline: true,
            maxRows: 10,
            size: 'small',
            style: {backgroundColor:textToTranslateBgColor},
            inputProps: {cols:27},
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != textToTranslate) {
                    textToTranslateOnChange(newText)
                }
            },
            onKeyUp: event => event.nativeEvent.keyCode == ESCAPE_KEY_CODE ? onCancel?.() : null,
        })),
        RE.If(hasValue(translation), () => RE.TextField({
            autoCorrect: 'off', autoCapitalize: 'none', spellCheck: 'false',
            value: translation,
            label: 'Translation',
            variant: 'outlined',
            multiline: true,
            maxRows: 10,
            size: 'small',
            style: {backgroundColor:translationBgColor, marginTop: '15px'},
            inputProps: {cols:27},
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != translation) {
                    translationOnChange(newText)
                }
            },
            onKeyUp: event => event.nativeEvent.keyCode == ESCAPE_KEY_CODE ? onCancel?.() : null,
        })),
        RE.If(hasValue(delay), () => RE.TextField({
            autoCorrect: 'off', autoCapitalize: 'none', spellCheck: 'false',
            value: delay,
            label: 'Delay',
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            size: 'small',
            style: {backgroundColor:delayBgColor, marginTop: '15px'},
            inputProps: {size:8},
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != delay) {
                    delayOnChange(newText)
                }
            },
            onKeyUp: event => event.nativeEvent.keyCode == ESCAPE_KEY_CODE ? onCancel?.() : null,
        })),
        renderButtons(),
    )
}
