import { registerRootComponent } from 'expo';
import './src/services/tasks'; // registers the background geofence + tracking tasks
import App from './App';

registerRootComponent(App);
