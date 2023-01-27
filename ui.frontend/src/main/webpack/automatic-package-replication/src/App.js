import { useState } from 'react';
import {Flex, View, Heading, Footer, Link, ProgressBar,  Divider, ActionButton, DialogTrigger, AlertDialog, TextField, ComboBox, Item, Picker, Form, defaultTheme, Provider} from '@adobe/react-spectrum';
import { usePackageManager } from './usePackageManager';

function App() {
  let [ready, setReady] = useState(false);
  let [title, setTitle] = useState();
  let [packagePath, setPackagePath] = useState();
  let [trigger, setTrigger] = useState();
  let [eventTopic, setEventTopic] = useState();
  let [eventFilter, setEventFilter] = useState();
  let [cronExpression, setCronExpression] = useState();

  
  let packagePaths = usePackageManager('ls')?.data;
  packagePaths = packagePaths.map(p => packageItem(p)) || [ {id: '/dev/null', name: 'Loading...'}];
  
  console.log(packagePaths);
  


  return (
    <Provider theme={defaultTheme}>
        <Flex direction="column" alignItems="center" width="100%" gap="size-400">

            <View backgroundColor="gray-900" height="size-500" width="100%">
                Header
            </View>

            <View width="600px">
                <Heading level={1}>Automatic Package Replication</Heading>

                <Form>

                        <TextField label="Title"
                                isRequired
                                necessityIndicator="label"
                                value={title}
                                onChange={setTitle}/>

                        {/* https://react-spectrum.adobe.com/react-spectrum/ComboBox.html */}
                        <ComboBox label="Package path"
                            isRequired
                            necessityIndicator="label"
                            defaultItems={packagePaths}
                            inputValue={packagePath}
                            onSelectionChange={setPackagePath}>
                            {item => <Item>{item.name}</Item>}
                        </ComboBox>

                        <Picker label="Trigger"
                            isRequired
                            necessityIndicator="label"
                            onSelectionChange={setTrigger}>
                        <Item key="cron">Cron</Item>
                        <Item key="event">Sling event</Item>
                        </Picker>

                        { trigger === 'event' && <>
                            <TextField label="Event topic"
                                isRequired
                                necessityIndicator="label"
                                value={eventTopic}
                                onChange={setEventTopic}/>

                            <TextField label="Event filter"
                                isRequired
                                necessityIndicator="label"
                                value={eventFilter}
                                onChange={setEventFilter}/>
                        </> }

                        { trigger === 'cron' &&

                            <>



                                <TextField label="Cron expression"
                                    isRequired
                                    necessityIndicator="label"
                                    value={cronExpression}
                                    onChange={setCronExpression}/>

                                <Link>
                                    <a href="https://crontab.cronhub.io/" target="_blank">
                                        Cron expression generator
                                    </a>
                                </Link>                                    
                            </>
                        }


                        <DialogTrigger>
                            <ActionButton marginTop="size-500" onPress={handleSave}>
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
                </Form>

            </View>

            <View width="100%" height="size-800">
                <Footer>&copy; All rights reserved.</Footer>
            </View>

        </Flex>

    </Provider>
  );



  function handleSave(e) {
    alert(title);
}
}

export default App;


function filterPackages(packages) {

    packages.filter()
}


function packageItem(package) {

    return {

        id: `/etc/packages/${package.group}/${package.downloadName}`,
        name: `${package.group} : ${package.downloadName}`
    }



}