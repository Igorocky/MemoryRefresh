"use strict";

const TEXT_READER_SETTINGS_LOCAL_STORAGE_KEY = "TextReader.settings"

const useTextReader = () => {

    const [voiceUri, setVoiceUri] = useStateFromLocalStorageString({key:TEXT_READER_SETTINGS_LOCAL_STORAGE_KEY+'.voiceUri', defaultValue:null, nullable:true})
    const [voiceObj, setVoiceObj] = useState(null)
    const [rate, setRate] = useStateFromLocalStorageNumber({key:TEXT_READER_SETTINGS_LOCAL_STORAGE_KEY+'.rate', min: 0.1, max: 10, defaultValue:1})
    const [pitch, setPitch] = useStateFromLocalStorageNumber({key:TEXT_READER_SETTINGS_LOCAL_STORAGE_KEY+'.pitch', min: 0, max: 2, defaultValue:1})
    const [volume, setVolume] = useStateFromLocalStorageNumber({key:TEXT_READER_SETTINGS_LOCAL_STORAGE_KEY+'.volume', min: 0, max: 1, defaultValue:1})
    const [textToSay, setTextToSay] = useState('')

    useEffect(() => {
        if (window.speechSynthesis) {
            window.speechSynthesis.onvoiceschanged = () => {
                const voiceObj = getVoiceObj(voiceUri)
                setVoiceObj(voiceObj)
                setVoiceUri(voiceObj?.voiceURI??null)
            }
        }
    }, [])

    function say(text) {
        var msg = new SpeechSynthesisUtterance()
        msg.voice = voiceObj
        msg.rate = rate
        msg.pitch = pitch
        msg.volume = volume
        msg.text = text
        msg.lang = "en"
        speechSynthesis.speak(msg)
    }

    function getVoiceObj(voiceUri) {
        const voices = window.speechSynthesis?.getVoices()
        if (voices?.length) {
            return voices.find(voice => voice.voiceURI === voiceUri || hasNoValue(voiceUri) && voice.default)
        }
    }

    function renderSettings() {
        return RE.table({style:{}},
            RE.tbody({},
                RE.tr({},
                    RE.td({style:{width: '90px'}},`Voice`),
                    RE.td({},
                        RE.Select(
                            {
                                value:voiceUri??"None",
                                onChange: event => {
                                    const newVoiceUri = event.target.value
                                    setVoiceObj(getVoiceObj(newVoiceUri))
                                    setVoiceUri(newVoiceUri)
                                },
                            },
                            window?.speechSynthesis?.getVoices()?.map(voice => RE.MenuItem(
                                {key: voice.voiceURI, value:voice.voiceURI, },
                                voice.name
                            ))??[]
                        )
                    ),
                ),
                RE.tr({},
                    RE.td({},`Rate ${rate}`),
                    RE.td({}, renderSlider({min:0.1, max:10, step: 0.1, value:rate, setValue: newValue => setRate(newValue)})),
                ),
                RE.tr({},
                    RE.td({},`Pitch ${pitch}`),
                    RE.td({}, renderSlider({min:0, max:2, step: 0.1, value:pitch, setValue: newValue => setPitch(newValue)})),
                ),
                RE.tr({},
                    RE.td({},`Volume ${volume}`),
                    RE.td({}, renderSlider({min:0, max:1, step: 0.1, value:volume, setValue: newValue => setVolume(newValue)})),
                ),
            )
        )
    }

    function renderSlider({min, max, step, value, setValue}) {
        return RE.div({style:{width:"170px"}},
            RE.Slider({
                value:value,
                onChange: (event, newValue) => setValue(newValue),
                step:step,
                min:min,
                max:max
            })
        )
    }

    function renderUserText() {
        return textField({
            value: textToSay,
            label: 'Text to say',
            variant: 'outlined',
            multiline: true,
            maxRows: 10,
            size: 'small',
            inputProps: {cols:20},
            style: {},
            onChange: event => {
                setTextToSay(event.nativeEvent.target.value)
            },
        })
    }

    function renderSayButton() {
        return iconButton({iconName:'volume_up', onClick: () => say(textToSay),})
    }

    function renderTextReaderConfig() {
        return RE.Paper({style:{padding:'5px', maxWidth:'270px', overflow:'hidden'}},
            RE.Container.col.top.left({},{},
                RE.Container.row.left.center({},{},
                    renderUserText(),
                    renderSayButton(),
                ),
                renderSettings()
            )
        )
    }

    return {say, renderTextReaderConfig, setTextToSay}
}
