"use strict";

const AddNotesView = ({query,openView,setPageTitle}) => {
    const {renderMessagePopup, showMessage, confirmAction} = useMessagePopup()

    const [allTags, setAllTags] = useState(null)
    const [allTagsMap, setAllTagsMap] = useState(null)

    useEffect(async () => {
        const {data:allTags} = await be.getAllTags({})
        setAllTags(allTags)
    }, [])

    useEffect(() => {
        if (allTags) {
            setAllTagsMap(allTags.reduce((a,t) => ({...a,[t.id]:t}), {}))
        }
    }, [allTags])

    async function showError({code, msg}) {
        return showMessage({text: `Error [${code}] - ${msg}`})
    }

    async function addNote({newNoteAttrs}) {
        const res = await be.saveNewNote(newNoteAttrs)
        if (!res.err) {
            return {clearText:true}
        } else {
            showError(res.err)
            return null
        }
    }

    if (hasNoValue(allTags)) {
        return "Loading tags..."
    } else {
        return re(UpdateNoteCmp,{
            allTags,
            allTagsMap,
            saveBtnText:'add',
            allowDelete: false,
            onSave: newNoteAttrs => addNote({newNoteAttrs})
        })
    }
}
