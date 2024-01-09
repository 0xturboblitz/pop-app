import { StyleSheet, Pressable } from 'react-native';
import Colors from '../../constants/Colors';
import { ExternalLink } from '../../components/ExternalLink';
import { Text, View } from '../../components/Themed';
import { MaterialCommunityIcons } from '@expo/vector-icons'; 
import * as PassportReader from '../../modules/passport-reader';

console.log('hello:', PassportReader.hello())

export default function TabOneScreen() {
  return (
    <View style={styles.container}>
      {/* <Text style={styles.title}>Tab One</Text>
      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" /> */}
      <View>
        <View style={styles.getStartedContainer}>
          <MaterialCommunityIcons name="numeric-1-circle-outline" size={24} color="black" />
          <Text
            style={styles.getStartedText}
            lightColor="rgba(0,0,0,0.8)"
            darkColor="rgba(255,255,255,0.8)">
            Scan the machine readable zone on the main page of your passport
          </Text>

          <MaterialCommunityIcons name="numeric-2-circle-outline" size={24} color="black" />
          <Text
            style={styles.getStartedText}
            lightColor="rgba(0,0,0,0.8)"
            darkColor="rgba(255,255,255,0.8)">
            Hold your passport against your device to read the biometric chip
          </Text>
        </View>

        <View style={styles.helpContainer}>
          <Pressable
            style={styles.helpLink}
            onPress={async () => {
              const a = await PassportReader.scan({})
              console.log('a:', a)
            }}
          >
            <Text style={styles.helpLinkText} lightColor={Colors.light.tint}>
              Open camera
            </Text>
          </Pressable>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%',
  },
  getStartedContainer: {
    alignItems: 'center',
    marginHorizontal: 50,
  },
  homeScreenFilename: {
    marginVertical: 7,
  },
  codeHighlightContainer: {
    borderRadius: 3,
    paddingHorizontal: 4,
  },
  getStartedText: {
    fontSize: 17,
    lineHeight: 24,
    marginBottom: 20,
    textAlign: 'center',
  },
  helpContainer: {
    marginTop: 15,
    marginHorizontal: 20,
    alignItems: 'center',
  },
  helpLink: {
    paddingVertical: 15,
  },
  helpLinkText: {
    textAlign: 'center',
  },
});
