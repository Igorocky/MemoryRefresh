"use strict";

const UpdateTranslateCardCmp = ({
                                    textToTranslate,textToTranslateOnChange,textToTranslateBgColor,
                                    translation,translationOnChange,translationBgColor,
                                    delay,delayOnChange,delayBgColor,
                                    onSave, saveBtnText = 'save', canSave = true,
                                    onCancel,
                                }) => {

    function renderSaveButton() {
        return RE.Button({
            variant: 'contained',
            color: 'primary',
            disabled: !canSave,
            onClick: onSave,
        }, saveBtnText ?? 'Save')
    }

    function renderCancelButton() {
        if (onCancel) {
            return RE.Button({variant: 'contained', color: 'default', onClick: onCancel}, 'Cancel')
        }
    }

    function renderButtons() {
        return RE.Container.row.left.center({}, {style: {marginRight: '3px'}},
            renderCancelButton(),
            renderSaveButton()
        )
    }

    return RE.Container.col.top.left({}, {style: {marginTop: '10px'}},
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
            style: {backgroundColor:translationBgColor},
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
            style: {backgroundColor:delayBgColor},
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
