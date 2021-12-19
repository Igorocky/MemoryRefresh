"use strict";

const CreateNewTagCmp = ({onSave}) => {
    const [expanded, setExpanded] = useState(false)
    const [tagName, setTagName] = useState('')

    async function save() {
        const result = await onSave({name: tagName})
        if (!result.err) {
            setTagName('')
            setExpanded(false)
        }
    }

    function cancel() {
        setExpanded(false)
    }

    if (expanded) {
        return RE.Container.row.left.center({},{},
            RE.IconButton({onClick:cancel},
                RE.Icon({style:{color:'black'}}, 'highlight_off')
            ),
            RE.TextField({
                autoCorrect:'off', autoCapitalize:'none', spellCheck:'false',
                value:tagName,
                variant:'outlined',
                autoFocus:true,
                size:'small',
                onChange: event => {
                    const newName = event.nativeEvent.target.value.trim()
                    if (newName != tagName) {
                        setTagName(newName)
                    }
                },
                onKeyUp: event =>
                    event.nativeEvent.keyCode == 13 ? save()
                        : event.nativeEvent.keyCode == 27 ? cancel()
                            : null,
            }),
            RE.IconButton({onClick: save}, RE.Icon({style:{color:'black'}}, 'save'))
        )
    } else {
        return RE.IconButton({onClick:()=>setExpanded(true)},
            RE.Icon({style:{color:'black'}}, 'add')
        )
    }
}
