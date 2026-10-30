import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  profileCard: {
    position: 'relative', 
    width: '85%',
    maxWidth: 400,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  closeButton: {
    position: 'absolute',
    top: 12,
    right: 14,
    zIndex: 10,
    padding: 4,
  },
  closeButtonText: {
    fontSize: 22,
    fontWeight: '600',
    color: '#666666',
    lineHeight: 22,
  },
  profileCardTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    paddingRight: 24, 
  },
  profileDetailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#EEEEEE',
  },
  profileLabel: {
    fontWeight: '600',
    color: '#555555',
  },
  profileValue: {
    color: '#222222',
  },
  logoutButton: {
    marginTop: 20,
    backgroundColor: '#D9534F',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  logoutButtonText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontSize: 16,
  },
});