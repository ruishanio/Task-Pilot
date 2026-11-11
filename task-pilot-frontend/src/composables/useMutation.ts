import { ref } from 'vue';
import { toast } from 'vue-sonner';
import { getErrorMessage } from '@/utils/format';
export function useMutation() {
  const busy = ref(false),
    error = ref('');
  async function run(
    action: () => Promise<unknown>,
    message?: string,
  ): Promise<boolean> {
    if (busy.value) return false;
    busy.value = true;
    error.value = '';
    try {
      await action();
      if (message) toast.success(message);
      return true;
    } catch (e) {
      error.value = getErrorMessage(e);
      toast.error(error.value);
      return false;
    } finally {
      busy.value = false;
    }
  }
  return { busy, error, run };
}
