"use strict";

const TagsView = ({query,openView,setPageTitle}) => {

    const [allTags, setAllTags] = useState(null)
    const [focusedTagId, setFocusedTagId] = useState(null)
    const [editMode, setEditMode] = useState(false)

    const {renderMessagePopup, showMessage, confirmAction} = useMessagePopup()

    useEffect(async () => {
        const {data:allTags} = await be.getAllTags({})
        setAllTags(allTags)
    }, [])

    function renderAllTags() {
        if (hasNoValue(allTags)) {
            return 'Loading tags...'
        } else if (allTags.length == 0) {
            return 'There are no tags.'
        } else {
            return RE.table({},
                RE.tbody({},
                    allTags.map(tag =>
                        RE.tr({key:tag.id, onClick: () => editMode ? null : setFocusedTagId(prev=> prev==tag.id?null:tag.id), style:{backgroundColor: focusedTagId === tag.id && !editMode ? 'lightgrey' : undefined}},
                            RE.td({}, renderTag({tag})),
                            RE.td({},
                                focusedTagId === tag.id && !editMode
                                    ? iconButton({iconName:'edit', onClick: () => setEditMode(true)})
                                    : null
                            ),
                            RE.td({},
                                focusedTagId === tag.id && !editMode
                                    ? iconButton({iconName:'delete', onClick: () => deleteTag({tag})})
                                    : null
                            ),
                        )
                    )
                )
            )
        }
    }

    function renderTag({tag}) {
        if (focusedTagId === tag.id && editMode) {
            return re(UpdateTagCmp,{
                tag,
                onCancel: e => {
                    e?.stopPropagation();
                    setEditMode(false)
                },
                onSave: async ({event,name}) => {
                    event?.stopPropagation();
                    const res = await be.updateTag({id: tag.id, name})
                    if (!res.err) {
                        if (res.data > 0) {
                            setAllTags(prev => prev.map(t=>t.id!=tag.id?t:{...t,name:name}))
                            setEditMode(false)
                        } else {
                            showError({code:-1,msg:`Internal error when updating a tag res.data=${res.data}`})
                        }
                    } else {
                        showError(res.err)
                    }
                },
            })
        } else {
            return tag.name
        }
    }

    async function showError({code, msg}) {
        return showMessage({text: `Error [${code}] - ${msg}`})
    }

    async function deleteTag({tag}) {
        if (await confirmAction({text: `Confirm deleting tag '${tag.name}'`})) {
            const res = await be.deleteTag({id: tag.id})
            if (!res.err) {
                if (res.data > 0) {
                    setAllTags(prev => prev.filter(t=>t.id!=tag.id))
                } else {
                    showError({code:-2,msg:`Internal error when deleting a tag res.data=${res.data}`})
                }
            } else {
                showError(res.err)
            }
        }
    }

    return RE.Container.col.top.left({style:{marginTop:'5px'}},{},
        re(CreateNewTagCmp,{
            onSave: async newTag => {
                const resp = await be.saveNewTag(newTag)
                if (resp.err) {
                    await showError(resp.err)
                    return {err:true}
                } else {
                    setAllTags(prev => [resp.data, ...prev])
                    return {}
                }
            }
        }),
        renderAllTags(),
        renderMessagePopup()
    )
}
