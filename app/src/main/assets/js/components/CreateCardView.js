"use strict";

const CreateCardView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, showMessage, showError} = useMessagePopup()

    const [textToTranslate, setTextToTranslate] = useState('')
    const [translation, setTranslation] = useState('')

    async function createCard() {
        const res = await be.saveNewTranslateCard({textToTranslate, translation})
        if (!res.err) {
            await showMessage({text: 'New card was saved.'})
            setTextToTranslate('')
            setTranslation('')
        } else {
            await showError(res.err)
        }
    }

    return RE.Fragment({},
        re(UpdateTranslateCardCmp,{
            textToTranslate,textToTranslateLabel:'Text to translate',onTextToTranslateChange: newValue => setTextToTranslate(newValue),
            translation,translationLabel:'Translation',onTranslationChange: newValue => setTranslation(newValue),
            onSave: createCard, canSave: textToTranslate.length && translation.length
        }),
        renderMessagePopup()
    )
}
