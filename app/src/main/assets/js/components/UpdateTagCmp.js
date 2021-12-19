"use strict";

const UpdateTagCmp = ({tag,onSave,onCancel}) => {
    const [tagName, setTagName] = useState(tag.name)

    function save(event) {
        onSave({event, name: tagName})
    }

    return RE.Container.row.left.center({},{},
        RE.IconButton({onClick:onCancel},
            RE.Icon({style:{color:'black'}}, 'highlight_off')
        ),
        RE.TextField({
            autoCorrect:'off', autoCapitalize:'none', spellCheck:'false',
            value:tagName,
            variant:'outlined',
            autoFocus:true,
            size:'small',
            onChange: event => {
                const newName = event.nativeEvent.target.value.replaceAll(' ', '')
                if (newName != tagName) {
                    setTagName(newName)
                }
            },
            onKeyUp: event =>
                event.nativeEvent.keyCode == 13 ? save()
                    : event.nativeEvent.keyCode == 27 ? onCancel()
                        : null,
        }),
        RE.IconButton(
            {
                onClick: save
            },
            RE.Icon({style:{color:'black'}}, 'save')
        )
    )
}
