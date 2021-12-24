"use strict";

const UpdateTranslateCardCmp = ({
                                    textToTranslate,onTextToTranslateChange,textToTranslateLabel,
                                    translation,onTranslationChange,translationLabel,
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
        return RE.Container.row.left.center({}, {style: {margin: '3px'}},
            renderCancelButton(),
            renderSaveButton()
        )
    }

    return RE.Container.col.top.left({}, {style: {marginTop: '10px'}},
        renderButtons(),
        RE.TextField({
            autoCorrect: 'off', autoCapitalize: 'none', spellCheck: 'false',
            value: textToTranslate,
            label: textToTranslateLabel,
            variant: 'outlined',
            autoFocus: true,
            multiline: true,
            maxRows: 10,
            size: 'small',
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != textToTranslate) {
                    onTextToTranslateChange(newText)
                }
            },
            onKeyUp: event => event.nativeEvent.keyCode == 27 ? onCancel?.() : null,
        }),
        RE.TextField({
            autoCorrect: 'off', autoCapitalize: 'none', spellCheck: 'false',
            value: translation,
            label: translationLabel,
            variant: 'outlined',
            multiline: true,
            maxRows: 10,
            size: 'small',
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != translation) {
                    onTranslationChange(newText)
                }
            },
            onKeyUp: event => event.nativeEvent.keyCode == 27 ? onCancel?.() : null,
        }),
        renderButtons(),
    )
}
