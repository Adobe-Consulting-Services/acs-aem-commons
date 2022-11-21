import { useState } from 'react';
import {Flex, View, Heading, Footer, Link, ProgressBar,  Divider, ActionButton, DialogTrigger, AlertDialog, TextField, ComboBox, Item, Picker, Form, defaultTheme, Provider, Dialog} from '@adobe/react-spectrum';
import ComponentDialog from './ComponentDialog';

function App() {
  let [ready, setReady] = useState(false);
  let [title, setTitle] = useState();
  let [packagePath, setPackagePath] = useState();
  let [trigger, setTrigger] = useState();
  let [eventTopic, setEventTopic] = useState();
  let [eventFilter, setEventFilter] = useState();
  let [cronExpression, setCronExpression] = useState();

  



  return (
    <Provider theme={defaultTheme}>
        <Flex direction="column" alignItems="center" width="100%" gap="size-400">

            <View backgroundColor="gray-900" height="size-500" width="100%">
                Header
            </View>

            <View width="600px">
                <Heading level={1}>Dispatcher Flush</Heading>

                <ComponentDialog></ComponentDialog>
              
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


