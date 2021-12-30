"use strict";

const EditTagCmp = ({tagId, tagName, tagNameTextFieldLabel, onSaved, onCanceled}) => {
    const {renderMessagePopup, showError} = useMessagePopup()
    const [newTagName, setNewTagName] = useState(tagName)

    async function create() {
        const res = await be.createTag({name:newTagName})
        if (res.err) {
            showError(res.err)
        } else {
            onSaved()
        }
    }

    async function update() {
        const res = await be.updateTag({tagId,name:newTagName})
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
