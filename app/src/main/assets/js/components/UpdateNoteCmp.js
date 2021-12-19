"use strict";

const UpdateNoteCmp = ({allTags,allTagsMap,note,onSave,onCancel,saveBtnText = 'save',allowDelete = true}) => {
    const [text, setText] = useState(note?.text??'')
    const [tags, setTags] = useState(note?.tagIds?.map(id=>allTagsMap[id])??[])
    const [isDeleted, setIsDeleted] = useState(note?.isDeleted??false)

    async function save() {
        const postSaveActions = await onSave({text, tagIds: tags.map(t => t.id), isDeleted})
        if (postSaveActions?.clearText) {
            setText('')
        }
    }

    function canSave() {
        return text.trim().length > 0 && tags.length > 0
    }

    function renderSaveButton() {
        return RE.Button({variant:'contained', color:'primary', disabled:!canSave(), onClick: save}, saveBtnText??'Save')
    }

    function renderCancelButton() {
        if (onCancel) {
            return RE.Button({variant:'contained', color:'default', onClick: onCancel}, 'Cancel')
        }
    }

    function renderButtons() {
        return RE.Container.row.left.center({},{style:{margin:'3px'}},
            renderCancelButton(),
            renderSaveButton()
        )
    }

    return RE.Container.col.top.left({},{},
        renderButtons(),
        RE.TextField({
            autoCorrect:'off', autoCapitalize:'none', spellCheck:'false',
            value:text,
            variant:'outlined',
            autoFocus:true,
            multiline: true,
            maxRows: 10,
            size:'small',
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText != text) {
                    setText(newText)
                }
            },
            onKeyUp: event => event.nativeEvent.keyCode == 27 ? onCancel() : null,
        }),
        RE.Paper({},
            re(TagSelector,{
                allTags,
                selectedTags: tags,
                onTagRemoved:tag=>setTags(prev=>prev.filter(t=>t.id!=tag.id)),
                onTagSelected:tag=>setTags(prev=>[...prev,tag]),
                label: 'Tag',
                color:'primary',
            })
        ),
        allowDelete?RE.FormControlLabel({
            control:RE.Checkbox({
                checked:isDeleted?true:false,
                onChange:() => setIsDeleted(prev=>!prev),
                color:'primary'
            }),
            label:'deleted'
        }):null,
        renderButtons(),
    )
}
