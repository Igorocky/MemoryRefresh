"use strict";

const ExportCardsCmp = ({usedTags, onExport, onCancelled}) => {

    const [fileName, setFileName] = useState("")
    const [tagsToSkip, setTagsToSkip] = useState(null)

    function renderFileName() {
        return textField({
            value: fileName,
            label: 'File name',
            variant: 'outlined',
            multiline: false,
            maxRows: 1,
            size: 'small',
            style: {marginTop:'20px'},
            autoFocus: true,
            onChange: event => {
                const newText = event.nativeEvent.target.value
                if (newText !== fileName) {
                    setFileName(newText)
                }
            }
        })
    }

    function renderTagsControls({allTags, label, tags, setTags}) {
        return RE.Container.row.left.center({},{},
            RE.FormGroup({style:{}},
                RE.FormControlLabel({
                    control: RE.Checkbox({
                        checked: hasValue(tags),
                        onChange: event => {
                            if (event.target.checked) {
                                setTags([])
                            } else {
                                setTags(null)
                            }
                        }
                    }),
                    label
                })
            ),
            RE.If(hasValue(tags), () => RE.Paper({style: {}},re(TagSelector,{
                allTags,
                selectedTags: tags,
                onTagRemoved:tag=> setTags(prev => prev.filter(t => t.id !== tag.id)),
                onTagSelected:tag=> setTags(prev => [...prev, tag]),
                label,
                color:'primary',
            })))
        )
    }

    function renderTagsToSkipControls() {
        return renderTagsControls({
            label: 'Skip tags', allTags: usedTags, tags: tagsToSkip, setTags: arg => setTagsToSkip(arg)
        })
    }

    function renderButtons() {
        return RE.Container.row.right.center({style:{marginTop:'15px'}},{},
            RE.Button({
                onClick: () => onExport({fileName, skipTags:tagsToSkip.map(t=>t.id)}),
                disabled: fileName.trim() === ''
            }, 'export'),
            RE.Button({onClick: () => onCancelled()}, 'cancel'),
        )
    }

    return RE.Container.col.top.left({},{},
        renderFileName(),
        renderTagsToSkipControls(),
        renderButtons()
    )
}
