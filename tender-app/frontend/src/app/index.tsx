import { Redirect } from 'expo-router';
import { restaurantsMock } from '../mocks/restaurants.mock';
import Home from './customer/home/home';

export default function IndexRoute() {
  const firstRestaurant = restaurantsMock[0];
  if (!firstRestaurant) return <Home />;

  return <Redirect href={{ pathname: '/customer/restaurant/[id]', params: { id: String(firstRestaurant.id) } }} />;
}
