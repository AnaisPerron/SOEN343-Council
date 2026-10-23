import Home from './customer/home';
export default function IndexRoute() {
  // TODO: Replace these placeholders with the real authentication and user-role state.
  const isLoggedIn = true;
  const userType = 'customer';
  if (isLoggedIn && userType === 'customer') {
    return <Home />;
  }
  return <Home />;
}
