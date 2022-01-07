"use strict";

const EditTagCmp = ({tagId, tagName, tagNameTextFieldLabel, onSaved, onCanceled}) => {
    const {renderMessagePopup, showError, showMessageWithProgress} = useMessagePopup()
    const [newTagName, setNewTagName] = useState(tagName)

    async function create() {
        const closeProgressIndicator = showMessageWithProgress({text: 'Saving new tag...'})
        const res = await be.createTag({name:newTagName})
        closeProgressIndicator()
        if (res.err) {
            showError(res.err)
        } else {
            onSaved()
        }
    }

    async function update() {
        const closeProgressIndicator = showMessageWithProgress({text: 'Updating tag...'})
        const res = await be.updateTag({tagId,name:newTagName})
        closeProgressIndicator()
        if (res.err) {
            showError(res.err)
        } else {
            onSaved()
        }
    }

    return RE.Fragment({},
        re(EditTagForm, {
            name: newTagName,
            tagNameTextFieldLabel,
            onNameChange: newTagName => setNewTagName(newTagName),
            onSave: () => hasNoValue(tagId) ? create() : update(),
            onCancel: onCanceled
        }),
        renderMessagePopup()
    )
}
