"use strict";

const ExportCardsCmp = ({onExport, onCancelled}) => {

    const [fileName, setFileName] = useState("")

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

    function renderButtons() {
        return RE.Container.row.right.center({style:{marginTop:'15px'}},{},
            RE.Button({onClick: () => onExport(fileName), disabled: fileName.trim() === ''}, 'export'),
            RE.Button({onClick: () => onCancelled()}, 'cancel'),
        )
    }

    return RE.Container.col.top.left({},{},
        renderFileName(),
        renderButtons()
    )
}
