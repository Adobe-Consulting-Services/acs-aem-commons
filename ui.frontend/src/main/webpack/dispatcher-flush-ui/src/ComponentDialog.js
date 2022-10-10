
import { useState } from 'react';
import {ProgressCircle, ActionButton, DialogTrigger, AlertDialog, TextField, ComboBox, Item, Form} from '@adobe/react-spectrum';
import {useAem} from './api/aem'

export default function ComponentDialog() {

  let [ready, setReady] = useState(false);
  let [title, setTitle] = useState();
  let [flushMethod, setFlushMethod] = useState();

  let { data, error } = useAem('');

  if (!data) {
    return (<ProgressCircle aria-label="Loading…" isIndeterminate />)
  }


return (<Form>

{/* https://react-spectrum.adobe.com/react-spectrum/ComboBox.html */}
<ComboBox label="Paths to flush"
    isRequired
    necessityIndicator="label"
    defaultInputValue="Invalidate cache"
    inputValue={flushMethod}
    onSelectionChange={setFlushMethod}>
    <Item value="ACTIVATE">Invalidate cache</Item>
    <Item value="DELETE">Delete cache</Item>
</ComboBox>



<DialogTrigger>
    <ActionButton marginTop="size-500" >
            Save
    </ActionButton>
    <AlertDialog
        title="Low Disk Space"
        variant="success"
        primaryActionLabel="Confirm">
        You are running low on disk space.
        Delete unnecessary files to free up space.
    </AlertDialog>
</DialogTrigger>
</Form>)

}